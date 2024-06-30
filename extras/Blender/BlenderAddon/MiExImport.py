import bpy
from bpy_extras.io_utils import ImportHelper
from bpy.props import StringProperty, EnumProperty
from bpy.types import Operator

import os, json, warnings

def setup_materials(object, data):
    pass

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
            setup_materials(mat, val)
        except:
            warnings.warn('Material failed: ' + key, UserWarning)
            raise

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
