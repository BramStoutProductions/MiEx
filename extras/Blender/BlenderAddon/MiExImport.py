import bpy
from bpy_extras.io_utils import ImportHelper
from bpy.props import StringProperty, EnumProperty
from bpy.types import Operator

import os, json, warnings

class setup_materials:

    def __init__(self,mat:bpy.types.Material, data, namespace:str):
        self.conn_to_make = []
        self.mat = mat
        self.namespace = namespace
        # delete all nodes in material
        self.mat.use_nodes = True

        for node in self.mat.node_tree.nodes:
            self.mat.node_tree.nodes.remove(node)

        for name, node_data in data['network'].items():
            try:
                self.import_node(name,node_data)
            except Exception as e:
                warnings.warn('Failed to create node: ' + name, UserWarning)
                raise e

        for conn in self.conn_to_make:
            print('Making connection: ' + str(conn))
            try:
                node0, attr0 = conn[0].split(".")
                node1, attr1 = conn[1].split(".")
                
                node0 = self.get_Node_By_Name(node0)
                node1 = self.get_Node_By_Name(node1)

                self.mat.node_tree.links.new(node0.outputs[attr0], node1.inputs[attr1])
            except Exception as e:
                warnings.warn('Failed to make connection: ' + str(conn), UserWarning)
                raise e

        for name, attr in data['terminals'].items():
            try:
                inputAttr = attr
                inputAttr = inputAttr.split("/")
                inputAttr = inputAttr[len(inputAttr)-1]
                output = self.get_output_node()
                self.mat.node_tree.links.new(self.get_Node_By_Name(inputAttr.split('.')[0]).outputs[inputAttr.split('.')[-1]], output.inputs[0])
            except Exception as e:
                warnings.warn('Failed to connect terminal: ' + name, UserWarning)
                raise e
        self.conn_to_make = []

    def import_node(self,name,data):
        node = self.mat.node_tree.nodes.new(data['type'])
        node.name = name
        node.label = name
        print('Importing node: ' + name)
        if 'attributes' in data:
            for attrName, attrData in data['attributes'].items():
                print('Importing attribute: ' + attrName)
                try:
                    if 'image' in attrData:
                        node.image = bpy.data.images.load(attrData['image']['value'])                  
                    if 'value' in attrData:
                        node[attrName] = attrData['value']
                    if 'connection' in attrData:
                        inputAttr = attrData["connection"]
                        inputAttr = inputAttr.split("/")
                        inputAttr = inputAttr[len(inputAttr)-1]
                        if self.namespace != '':
                            self.conn_to_make.append((self.namespace + ":" + inputAttr, node.name + "." + attrName))
                        else:
                            self.conn_to_make.append((inputAttr, node.name + "." + attrName))
                    # Copilot translation from Maya to Blender, not sure how correct.
                    if "keyframes" in attrData:
                        keyframes = attrData["keyframes"]
                        numFrames = len(keyframes) // 2
                        i = 0
                        if attrData["type"] == "float":
                            while i < numFrames:
                                node.keyframe_insert(data_path=name, frame=keyframes[i*2], value=keyframes[i*2+1])
                                node.animation_data.action.fcurves[-1].keyframe_points[-1].interpolation = 'CONSTANT'
                                i += 1
                        elif attrData["type"] == "float2":
                            childAttrs = [prop.identifier for prop in node.bl_rna.properties if not prop.is_readonly]
                            numCompounds = len(childAttrs)
                            j = 0
                            while j < numCompounds:
                                i = 0
                                while i < numFrames:
                                    node.keyframe_insert(data_path=childAttrs[j], frame=keyframes[i*2], value=keyframes[i*2+1][j])
                                    node.animation_data.action.fcurves[-1].keyframe_points[-1].interpolation = 'CONSTANT'
                                    i += 1
                                j += 1
                        
                except Exception as e:
                    warnings.warn('Failed to set attribute: ' + attrName, UserWarning)
                    raise e
        print('Node imported: ' + name)

    def get_output_node(self):
        material_output = None
        for node in self.mat.node_tree.nodes:
            if node.type == "OUTPUT_MATERIAL":
                material_output = node
                break
        if material_output is None:
            material_output = self.mat.node_tree.nodes.new('ShaderNodeOutputMaterial')
        return material_output

    def get_Node_By_Name(self, name:str):
        for node in self.mat.node_tree.nodes:
            if node.name == name:
                return node
        raise Exception("Node not found: " + name)

def read_data(context, filepath, options: dict):

    directory = os.path.dirname(filepath)
    filename = os.path.basename(filepath).split('.')[0]
    # Look for material json file
    materialJson = os.path.join(directory, filename + '_materials.json')
    
    if not os.path.exists(materialJson):
        
        # Warning popup
        message = "Material file not found: " + materialJson
        warnings.warn(message, UserWarning)
        
        return {'CANCELLED'}
    
    # Read material json file
    with open(materialJson) as f:
        materials = json.load(f)

    # Get mesh list before import
    meshes_ = set(o.data for o in bpy.context.scene.objects if o.type == 'MESH')

    bpy.ops.wm.usd_import(filepath=filepath)

    # Make a filtered list of meshes that were imported
    meshes = set(o.data for o in bpy.context.scene.objects if o.type == 'MESH' and o.data not in meshes_)
    print(options)
    # Filter meshes based on import type
    for mesh in meshes:
        if options['import_type'] != 'both':
            # Delete mesh if it is not the type we want
            if  str(mesh.name).endswith('_proxy') and options['import_type'] == 'render':
                obj = bpy.data.objects[str(mesh.name)]
                bpy.data.objects.remove(obj, do_unlink=True)
            elif not str(mesh.name).endswith('_proxy') and options['import_type'] == 'proxy':
                obj = bpy.data.objects[str(mesh.name)]
                bpy.data.objects.remove(obj, do_unlink=True)
        else:
            # Show proxy in viewport but not render
            if str(mesh.name).endswith('_proxy'):
                mesh.hide_render = True
            # Show render in render but not viewport
            elif not str(mesh.name).endswith('_proxy'):
                mesh.hide_viewport = True
        
        if options['namespace'] != '':
            # Add namespace to mesh name
            mesh.name = options['namespace'] + '_' + str(mesh.name)

    # Finally we can set up mats
    for key,val in materials.items():
        try:
            mat = bpy.data.materials[key]

            if mat is None:
                warnings.warn('Material not found: ' + key, UserWarning)
                continue
            print('Setting up material: ' + key)
            setup_materials(mat, val, options['namespace'])
            print('Material set up: ' + key)

            if options['namespace'] != '':
                mat.name = options['namespace'] + '_' + key
        except Exception as e:
            warnings.warn('Material failed: ' + key, UserWarning)
            raise e

    return {'FINISHED'}

class MiexImport(Operator, ImportHelper):
    bl_idname = "mieximport.world"
    bl_label = "Import MiEx (.usd)"
    filename_ext = ".usd"  # Specify the file extension

    filter_glob: StringProperty(
        default="*.usd",
        options={'HIDDEN'},
        maxlen=255
    )
    
    obj_namespace: StringProperty(name="Namespace")
    import_type: EnumProperty(
        name="Import type",
        description="Import either proxy or render variant",
        items=(
            ('proxy', "Proxy", "Import proxy models only"),
            ('render', "Render", "Import render models only"),
            ('both', "Both", "Import both proxy and render models")
        ),
    )

    def execute(self, context):

        options = {
            'namespace': self.obj_namespace,
            'import_type': self.import_type,
        }

        return read_data(context, self.filepath, options)

def menu_func_import(self, context):
    self.layout.operator(MiexImport.bl_idname, text="MiEx (.usd)")

def register():
    bpy.utils.register_class(MiexImport)
    bpy.types.TOPBAR_MT_file_import.append(menu_func_import)

def unregister():
    bpy.utils.unregister_class(MiexImport)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func_import)

if __name__ == "__main__":
    register()
    # Test call (invoke the operator)
    bpy.ops.mieximport.world('INVOKE_DEFAULT')
