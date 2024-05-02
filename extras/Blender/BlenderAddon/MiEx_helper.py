import bpy
import traceback
import os


bl_info = {
    "name" : "MiEx Helper",
    "author" : "BlueEvilGFX, michael212345",
    "version" : (0, 1, 3),
    "blender" : (4, 1, 0),
    "description" : "This addon preps the world created by MiEx",
    "wiki_url" : "https://github.com/BramStoutProductions/MiEx/",
}


# ---
# This addon is licensed under GPL
# It serves as a helper utility for importing MiEx worlds.
# Code written by BlueEvilGFX and partially michael212345.
# ---


class PrepWorld(bpy.types.Operator):
    bl_idname = "miex.prep"
    bl_label = "Prep MiEx World"
    bl_options = {'REGISTER', 'UNDO'}

    resize : bpy.props.BoolProperty(
        default = True
    ) #type: ignore

    animated_textures : bpy.props.BoolProperty(
        default = True
    ) #type: ignore

    frame_spacing : bpy.props.IntProperty(
        default = 3,
        description = 'how many frames between image changes should be for animation',
        min = 1,
        max = 40,
        soft_max = 20
    ) #type: ignore

    MAT_COLOUR_MAPED = {'MAT_minecraft_block_grass_block_side_BIOME'}

    @classmethod
    def poll(self, context):
        return context.object.type == 'EMPTY'

    def invoke(self, context, event):
        return context.window_manager.invoke_props_dialog(self, width = 400)

    def draw(self, context):
        layout = self.layout
        layout.prop(self, 'resize', text = 'resize world')

        box = layout.box()
        box.prop(self, 'animated_textures', text = 'use animated textures')
        row = box.row()
        row.enabled = self.animated_textures
        row.prop(self, 'frame_spacing', slider = True)

    def execute(self, context) :
        if self.resize:
            self.resize_world()

        materials = self.get_materials()
        
        # edit the materials
        for obj, mat in materials.items():
            if mat.node_tree is None:
                continue

            try:
                self.clean_material(mat)

                # set basic material settings
                nodes = mat.node_tree.nodes
                img_node = nodes['Image Texture']
                mat.blend_method = "HASHED"
                mat.shadow_method = "HASHED"
                img_node.interpolation = 'Closest'

                # animation
                size = img_node.image.size
                # is vertical and multiple of horizontal
                if size[0] < size[1] and size[1] % size[0] == 0:
                    # if animation if false -> only set correct UVs
                    self.add_animation(mat, img_node, size)

                # Colour map
                if not 'Cd' in obj.data.attributes or 'snow' in mat.name:
                    continue
                
                if any([key for key in self.MAT_COLOUR_MAPED if key in mat.name]):
                    self.block_side_adjustment(mat, nodes)
                else:
                    self.basic_adjustment(mat, nodes)

            except Exception:
                print("MAT ERROR: on obj", mat.name)
                print(traceback.format_exc())
        return {'FINISHED'}

    def get_materials(self):
        materials = dict()
        def traverse_children(child_obj):
            if child_obj.material_slots:
                materials[child_obj] = child_obj.material_slots[0].material
            else:
                for grandchild in child_obj.children:
                    traverse_children(grandchild)

        for child in bpy.context.object.children:
            traverse_children(child)

        return materials
    
    def clean_material(self, mat):
        nodes = mat.node_tree.nodes
        for node in nodes:
            if node.type not in {'TEX_IMAGE', 'BSDF_PRINCIPLED', 'OUTPUT_MATERIAL', 'UVMAP'}:
                mat.node_tree.nodes.remove(node)
        # reconnecting image to bsdf
        img_node = nodes['Image Texture']
        bsdf_node = nodes['Principled BSDF']
        links = mat.node_tree.links
        links.new(img_node.outputs['Color'], bsdf_node.inputs['Base Color'])
        links.new(img_node.outputs['Alpha'], bsdf_node.inputs['Alpha'])

    def resize_world(self):
        bpy.context.object.scale = [0.0925, 0.0925, 0.0925]
    
    def block_side_adjustment(self, mat, nodes):
        links = mat.node_tree.links
        attr_node = nodes.new('ShaderNodeAttribute')
        attr_node.attribute_name = 'Cd'

        bsdf_node = nodes['Principled BSDF']
        img_node = nodes['Image Texture']
        img_path = img_node.image.filepath

        # get overlay texture
        overlay_path = os.path.splitext(img_path)[0] + "_overlay.png"

        # Check if overlay exists
        if not os.path.exists(overlay_path):
            return print(f"MiEx: Overlay texture not found: {overlay_path}")
    
        # Get uv map node
        uv_map = mat.node_tree.nodes.get('UV Map')
        
        #Create a new image node
        overlay_node = mat.node_tree.nodes.new('ShaderNodeTexImage')
        overlay_node.image = bpy.data.images.load(overlay_path)
        overlay_node.interpolation = 'Closest'
        
        # Connect uv map to overlay node
        links.new(uv_map.outputs['UV'], overlay_node.inputs['Vector'])
        
        # Create mix color node, set it to multiply
        multiply_mix_node = mat.node_tree.nodes.new('ShaderNodeMixRGB')
        multiply_mix_node.blend_type = 'MULTIPLY'
        multiply_mix_node.inputs['Fac'].default_value = 1.0
        
        # Link overlay with attribute node
        links.new(attr_node.outputs['Color'], multiply_mix_node.inputs[1])
        links.new(overlay_node.outputs['Color'], multiply_mix_node.inputs[2])
        
        # Create mix color node, set it to mix
        mix_node = mat.node_tree.nodes.new('ShaderNodeMixRGB')
        mix_node.blend_type = 'MIX'
        
        # Create invert color node to invert alpha values
        invert_node = mat.node_tree.nodes.new('ShaderNodeInvert')
        
        # Link remaining nodes
        links.new(multiply_mix_node.outputs['Color'], mix_node.inputs[1])
        links.new(img_node.outputs['Color'], mix_node.inputs[2])
        links.new(overlay_node.outputs['Alpha'], invert_node.inputs['Color'])
        links.new(invert_node.outputs['Color'], mix_node.inputs['Fac'])
        links.new(mix_node.outputs['Color'], bsdf_node.inputs['Base Color'])

        # repositioning of nodes
        uv_map.location = (-1040, 120)
        nodes['Image Texture'].location = (-840, 340)
        nodes['Image Texture.001'].location = (-840, 60)
        invert_node.location = (-560, 160)
        multiply_mix_node.location = (-360, 140)
        mix_node.location = (-180, 300)
        attr_node.location = (-560, -20)

    def basic_adjustment(self, mat, nodes):
        # Create a attribute node
        attr_node = nodes.new('ShaderNodeAttribute')
        attr_node.attribute_name = 'Cd'
        
        # Get BSDF
        bsdf_node = nodes.get('Principled BSDF')
        base_color = bsdf_node.inputs['Base Color'].links[0].from_node
        
        # Create mix color node set it to multiply
        mix_node = nodes.new('ShaderNodeMixRGB')
        mix_node.blend_type = 'MULTIPLY'
        mix_node.inputs['Fac'].default_value = 1.0
        
        # Link the nodes
        links = mat.node_tree.links
        links.new(base_color.outputs['Color'], mix_node.inputs[1])
        links.new(attr_node.outputs['Color'], mix_node.inputs[2])
        links.new(mix_node.outputs['Color'], bsdf_node.inputs['Base Color'])

        # Repositioning of nodes
        nodes['UV Map'].location = (-840, 300)
        nodes['Image Texture'].location = (-640, 300)
        attr_node.location = (-360, 140)
        nodes['Mix (Legacy)'].location = (-180, 300)

    def add_animation(self, mat, img_node, size):
        nodes = mat.node_tree.nodes

        # mapping node
        map_node = nodes.new("ShaderNodeMapping")
        map_node.location = (-480, 300)
        
        vec_math = nodes.new("ShaderNodeVectorMath")
        vec_math.location = (-680, 40)
        vec_math.operation = "MULTIPLY"
        vec_math.inputs[1].default_value[1] = 1
        
        value_node = nodes.new("ShaderNodeValue")
        value_node.location = (-880, 40)
        
        links = mat.node_tree.links
        links.new(nodes['UV Map'].outputs[0], map_node.inputs["Vector"])
        links.new(value_node.outputs[0], vec_math.inputs[0])
        links.new(vec_math.outputs[0], map_node.inputs["Location"])
        links.new(map_node.outputs[0], img_node.inputs["Vector"])
        
        # set UV size
        frames = size[1]/size[0]
        map_node.inputs["Scale"].default_value[1] = 1/frames

        # setup animation
        if self.animated_textures:
            value_node = mat.node_tree.nodes.get("Value")
            frames = img_node.image.size[1]/img_node.image.size[0]
            dr = value_node.outputs[0].driver_add("default_value")
            dr.driver.expression = f'round(frame/{self.frame_spacing})/{frames}'
            bpy.context.view_layer.update()       

def menu_func(self, context):
    self.layout.operator('miex.prep')

def register():
    bpy.utils.register_class(PrepWorld)
    bpy.types.VIEW3D_MT_object.append(menu_func)

def unregister():
    bpy.types.VIEW3D_MT_object.remove(menu_func)
    bpy.utils.unregister_class(PrepWorld)