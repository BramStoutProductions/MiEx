import unreal
import os, json
import tkinter as tk
from tkinter import filedialog

editor_util = unreal.EditorUtilityLibrary()
editor_asset_lib = unreal.EditorAssetLibrary()
material_lib = unreal.MaterialEditingLibrary()
asset_tools = unreal.AssetToolsHelpers.get_asset_tools()

HLSLCODE= """
const float animationLength = 11.0;
const float fps = {FPS};
const int keyframes[16] = {
    0, 1,
    1, 1,
    2, 1,
    3, 4,
    4, 1,
    5, 1,
    6, 1,
    7, 1
};
const int numKeyframes = 8;
const int numTexFrames = 8;

float localTime = (Time * fps) / animationLength;
localTime = localTime - floor(localTime);
localTime = localTime * animationLength;
int i = 0;
float t = localTime;
for(; i < numKeyframes; ++i){
    if(t < keyframes[i * 2 + 1]){
        t /= keyframes[i * 2 + 1];
        break;
    }
    t -= keyframes[i * 2 + 1];
}
int texFrame = keyframes[i * 2];
int texFrame2 = keyframes[((i + 1) % numKeyframes) * 2];

float2 uv = UVs + float2(0.0, (float) texFrame);
uv /= float2(1.0, (float) numTexFrames);
float2 uv2 = UVs + float2(0.0, (float) texFrame2);
uv2 /= float2(1.0, (float) numTexFrames);

float4 color1 = Texture2DSample(texture, textureSampler, uv);
float4 color2 = Texture2DSample(texture, textureSampler, uv2);

return color1 * (1.0 - t) + color2 * t;
"""

def select_usd_file():
    root = tk.Tk()
    root.withdraw()  # Hide the main window
    file_path = filedialog.askopenfilename(filetypes=[("USD files", "*.usd")],title="Select USD file")
    return file_path

def in_engine_destination_path():
    root = tk.Tk()
    root.withdraw()  # Hide the main window
    file_path = filedialog.askdirectory(initialdir=unreal.Paths.project_dir(),title="Select destination folder")
    return '/'+unreal.Paths.make_path_relative_to(file_path.replace('\\','/'),unreal.Paths.project_dir()) if file_path else None # Make the path relative to the project directory if a path is selected

def change_material_instance_parent(material_instance_path, new_parent_material_path):
    material_instance = unreal.load_asset(material_instance_path)
    new_parent_material = unreal.load_asset(new_parent_material_path)

    material_lib.set_material_instance_parent(material_instance, new_parent_material)

class Setup_Material:
    def __init__(self, data, mat_name, Dir, inEngineDir):
        self.mat_path = os.path.join(inEngineDir, os.path.basename(Dir).split('.')[0],'Materials').replace('\\','/')
        self.data = data
        self.Dir = Dir
        self.inEngineDir = inEngineDir
        self.mat_name = mat_name
    
        self.hasTransparency = False
        self.importTask = []
        self.createdNodes = {}
    
        editor_asset_lib.make_directory(self.mat_path)
        editor_asset_lib.make_directory(os.path.join(self.inEngineDir, os.path.basename(self.Dir).split('.')[0],'Textures').replace('\\','/'))
        
        self.conn_to_make = []
    
        # Create a new material
        asset_tools.create_asset(self.mat_name, self.mat_path, unreal.Material, unreal.MaterialFactoryNew())

        # Load the material
        self.material:unreal.Material = unreal.load_asset(os.path.join(self.mat_path, self.mat_name).replace('\\','/'))
        unreal.log(type(self.material))
        for name, node_data in data['network'].items():
            try:
                self.import_node(name,node_data)
            except Exception as e:
                unreal.log_warning('Failed to create node: ' + name)
                raise e
        
        for conn in self.conn_to_make:
            try:
                outputNodeName = conn[0].split(".")[0]
                outputAttrName = conn[0].split(".")[1]
                if outputAttrName == "out":
                    outputAttrName = ""
                inputNodeName = conn[1].split(".")[0]
                inputAttrName = conn[1].split(".")[1]
                if inputAttrName == "in":
                    inputAttrName = ""
                
                outputNode = self.createdNodes[outputNodeName]

                if inputNodeName in self.createdNodes:
                    inputNode = self.createdNodes[inputNodeName]
                    unreal.MaterialEditingLibrary.connect_material_expressions(outputNode, outputAttrName, inputNode, inputAttrName)
                else:
                    unreal.MaterialEditingLibrary.connect_material_property(outputNode, outputAttrName, getattr(unreal.MaterialProperty, inputAttrName))
            except Exception as e:
                unreal.log_warning('Failed to connect node: ' + conn[0] + ' to ' + conn[1])
                raise e
        
        # Check if asset exists
        matInstancePath = os.path.join(self.mat_path, 'MI_'+self.mat_name).replace('\\','/')
        
        if unreal.EditorAssetLibrary.does_asset_exist(matInstancePath):
            matInstance = unreal.load_asset(matInstancePath)
            unreal.MaterialEditingLibrary.set_material_instance_parent(matInstance, self.material)

        matInstancePath = os.path.join(self.mat_path, 'MI_'+self.mat_name+'_TwoSided').replace('\\','/')
        
        if unreal.EditorAssetLibrary.does_asset_exist(matInstancePath):
            matInstance = unreal.load_asset(matInstancePath)
            unreal.MaterialEditingLibrary.set_material_instance_parent(matInstance, self.material)
            
            propertyOverrides = unreal.MaterialInstanceBasePropertyOverrides()
            propertyOverrides.set_editor_property('override_two_sided', True)
            propertyOverrides.set_editor_property('two_sided', True)
            matInstance.set_editor_property("base_property_overrides", propertyOverrides)

        # save the material
        unreal.MaterialEditingLibrary.layout_material_expressions(self.material)
        unreal.MaterialEditingLibrary.recompile_material(self.material)

    def import_node(self, name:str, node_data:dict):
        isMaterial = True if 'Material' == node_data['type'] else False
        unreal.log_warning(f'Importing {name} isMaterial: {str(isMaterial)}')
        
        expression = None
        if isMaterial == False:
            texObj = None
            if node_data['type'] == 'MC_Animate' :
                
                time = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionTime, 0,0)
                texCoord = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionTextureCoordinate, 0,0)
                custom = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionCustom, 0,0)
                custom.set_editor_property('output_type', unreal.CustomMaterialOutputType.CMOT_FLOAT4)
                texObj = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionTextureObject, 0,0)
                customInput1 = unreal.CustomInput()
                customInput1.set_editor_property('input_name','texture')
                customInput2 = unreal.CustomInput()
                customInput2.set_editor_property('input_name','UVs')
                customInputs3 = unreal.CustomInput()
                customInputs3.set_editor_property('input_name','Time')
                
                custom.set_editor_property('inputs', [customInput1, customInput2, customInputs3])
                
                unreal.MaterialEditingLibrary.connect_material_expressions(texCoord, '', custom, 'UVs')
                unreal.MaterialEditingLibrary.connect_material_expressions(time, '', custom, 'Time')
                expression = custom
                self.createdNodes[name] = custom

            elif node_data['type'] == 'MC_Interpolate':
                time = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionTime, 0,0)
                texCoord = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionTextureCoordinate, 0,0)
                custom = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionCustom, 0,0)
                custom.set_editor_property('output_type', unreal.CustomMaterialOutputType.CMOT_FLOAT4)
                texObj = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionTextureObject, 0,0)
                customInput1 = unreal.CustomInput()
                customInput1.set_editor_property('input_name','texture')
                customInput2 = unreal.CustomInput()
                customInput2.set_editor_property('input_name','UVs')
                customInputs3 = unreal.CustomInput()
                customInputs3.set_editor_property('input_name','Time')
                
                custom.set_editor_property('inputs', [customInput1, customInput2, customInputs3])
                
                unreal.MaterialEditingLibrary.connect_material_expressions(texCoord, '', custom, 'UVs')
                unreal.MaterialEditingLibrary.connect_material_expressions(time, '', custom, 'Time')
                expression = custom
                self.createdNodes[name] = custom

            else:
                expression = unreal.MaterialEditingLibrary.create_material_expression(self.material, getattr(unreal,node_data['type']), 0,0)
                self.createdNodes[name] = expression
            
        if 'attributes' in node_data:
            for attrName, attrData in node_data['attributes'].items():
                if isMaterial and attrName == "blend_mode":
                    self.material.set_editor_property('blend_mode', getattr(unreal.BlendMode, node_data['attributes']['blend_mode']['value']))
                    
                if attrData["type"] == "asset": # Since unreal is very picky with how things are layed out, for now i will only support texture importing
                    imagePath = attrData['value']
                    if not os.path.exists(imagePath):
                        imagePath = os.path.join(self.rootDir, imagePath)
                        if not os.path.exists(imagePath):
                            unreal.log_warning('Image does not exist: ' + imagePath)
                            continue # Image doesn't exist, so just don't load it.

                    task = unreal.AssetImportTask()
                    task.filename = imagePath
                    task.destination_path = os.path.join(self.inEngineDir, os.path.basename(self.Dir).split('.')[0],'Textures').replace('\\','/')
                    task.destination_name = name
                    task.automated = True
                    task.replace_existing = False
                    task.options = unreal.TextureFactory()
                    task.save = True
                    
                    asset_tools.import_asset_tasks([task])
                    texture = unreal.load_asset(task.get_editor_property('imported_object_paths')[0])
                    if 'lod_group' in node_data['attributes']:
                        texture.set_editor_property('lod_group', getattr(unreal.TextureGroup, node_data['attributes']['lod_group']['value']))
                        
                    if texObj is not None:
                        texObj.set_editor_property(attrName, texture)
                        unreal.MaterialEditingLibrary.connect_material_expressions(texObj, '', expression, attrName)
                    else:
                        expression.set_editor_property(attrName, texture)
                    

                elif 'value' in attrData:
                    unreal.log(F'{name} isMaterial:  {str(isMaterial)}')
                    if 'type' in attrData and isMaterial:
                        if attrData['type'] == 'Color':
                            exp = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionConstant3Vector, 0,0)
                            exp.set_editor_property('constant', unreal.LinearColor(attrData['value'][0], attrData['value'][1], attrData['value'][2], 1))
                            unreal.MaterialEditingLibrary.connect_material_property(exp, '', getattr(unreal.MaterialProperty, attrName))
                        elif attrData['type'] == 'float':
                            exp = unreal.MaterialEditingLibrary.create_material_expression(self.material, unreal.MaterialExpressionConstant, 0,0)
                            exp.set_editor_property('r', attrData['value'])
                            unreal.MaterialEditingLibrary.connect_material_property(exp, '', getattr(unreal.MaterialProperty, attrName))
                    else:
                        try:
                            expression.set_editor_property(attrName, attrData['value'])
                        except:
                            unreal.log_warning('Failed to set property: ' + attrName)
                

                elif 'connection' in attrData:
                    inputAttr = attrData["connection"]
                    inputAttr = inputAttr.split("/")
                    inputAttr = inputAttr[len(inputAttr)-1]
                    self.conn_to_make.append((inputAttr, name + "." + attrName))
                
                elif "anim_data" in attrData:
                    expression.set_editor_property('code', attrData['value'])

def main():

    usd_file_path = select_usd_file()
    in_engine_destination = in_engine_destination_path() # Remove the Content/ part of the path

    if not in_engine_destination == None:
        in_engine_destination = in_engine_destination.replace('Content','Game')
    else:
        in_engine_destination = '/Game/USDImport'

    if not usd_file_path:
        unreal.log_error("No file selected")
        return
        
    if not os.path.exists(usd_file_path):
        unreal.log_error("File does not exist")
        return
    
    directory = os.path.dirname(usd_file_path)
    filename = os.path.basename(usd_file_path).split('.')[0]
    
    materialJson = os.path.join(directory, filename + '_materials.json')
    
    if not os.path.exists(materialJson):
        unreal.log_error("Material json does not exist")
        return
    
    project_dir = unreal.Paths.project_dir()
    # Check if assets exist in /Game/USDImport
    if not in_engine_destination:
        usd_import_dir = os.path.join(project_dir, "Content", "USDImport")
        if not os.path.exists(usd_import_dir):
            os.makedirs(usd_import_dir)

    task = unreal.AssetImportTask()
    task.filename = usd_file_path
    task.destination_path = in_engine_destination # If no destination is selected, import to /Game/USDImport
    task.replace_existing = True
    task.automated = True
    
    options = unreal.UsdStageImportOptions()
    options.import_only_used_materials = False
    options.render_context_to_import = unreal.Name('universal')
    options.material_purpose = 'allPurpose'
    options.override_stage_options = True
    options.merge_identical_material_slots = True
    options.reuse_identical_assets = True
    
    stage_options = unreal.UsdStageOptions()
    stage_options.meters_per_unit = 0.0625
    stage_options.up_axis = unreal.UsdUpAxis.Y_AXIS
    
    options.stage_options = stage_options
    
    options.nanite_triangle_threshold = 2147483647 # Max int to disable nanite, user can enable manually
    
    task.options = options
    task.save = True

    asset_tools.import_asset_tasks([task])
    
    with open(materialJson, 'r') as f:
        materials = json.load(f)
            
    for key, value in materials.items():
        Setup_Material(value, key, usd_file_path, in_engine_destination)
 
if __name__ == "__main__":
    main()