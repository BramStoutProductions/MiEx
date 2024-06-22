import bpy
from bpy_extras.io_utils import ImportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty
from bpy.types import Operator

import os, json, warnings

def read_some_data(context, filepath, use_some_setting):
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
        
    print(materials)
    return {'FINISHED'}

class MiexImport(Operator, ImportHelper):
    bl_idname = "mieximport.world"
    bl_label = "Import MiExImport (.usd)"
    filename_ext = ".usd"  # Specify the file extension

    filter_glob: StringProperty = StringProperty(
        default="*.usd",
        options={'HIDDEN'},
        maxlen=255
    )
    
    # Define the use_setting attribute
    use_setting: BoolProperty = BoolProperty(
        name="Use Setting",
        description="Some setting description",
        default=True
    )

    def execute(self, context):

        return read_some_data(context, self.filepath, self.use_setting)

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
