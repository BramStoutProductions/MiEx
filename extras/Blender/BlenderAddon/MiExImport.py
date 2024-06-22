import bpy
from bpy_extras.io_utils import ImportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty
from bpy.types import Operator

def read_some_data(context, filepath, use_some_setting):
    print("running read_some_data...")
    f = open(filepath, 'r', encoding='utf-8')
    data = f.read()
    f.close()
    # You can process the data here
    print(data)
    return {'FINISHED'}

class ImportSomeData(Operator, ImportHelper):
    bl_idname = "mieximport.world"
    bl_label = "Import MiExImport (.usd)"
    filename_ext = ".usd"  # Specify the file extension

    filter_glob: StringProperty = StringProperty(
        default="*.usd",
        options={'HIDDEN'},
        maxlen=255
    )

    def execute(self, context):
        return read_some_data(context, self.filepath, self.use_setting)

def menu_func_import(self, context):
    self.layout.operator(ImportSomeData.bl_idname, text="MiExImport (.usd)")

def register():
    bpy.utils.register_class(ImportSomeData)
    bpy.types.TOPBAR_MT_file_import.append(menu_func_import)

def unregister():
    bpy.utils.unregister_class(ImportSomeData)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func_import)

if __name__ == "__main__":
    register()
    # Test call (invoke the operator)
    bpy.ops.mieximport.world('INVOKE_DEFAULT')
