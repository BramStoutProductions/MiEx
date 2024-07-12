import unreal
import os
import tkinter as tk
from tkinter import filedialog

def select_usd_file():
    root = tk.Tk()
    root.withdraw()  # Hide the main window
    file_path = filedialog.askopenfilename(filetypes=[("USD files", "*.usd")],title="Select USD file")
    return file_path

def in_engine_destination_path():
    root = tk.Tk()
    root.withdraw()  # Hide the main window
    file_path = filedialog.askdirectory(initialdir=unreal.Paths.project_dir(),title="Select destination folder")
    return '/'+unreal.Paths.make_path_relative_to(file_path,unreal.Paths.project_dir()) if file_path else None # Make the path relative to the project directory if a path is selected

def main():

    usd_file_path = select_usd_file()
    in_engine_destination = in_engine_destination_path().replace('Content','Game') # Remove the Content/ part of the path

    if not usd_file_path:
        unreal.log_error("No file selected")
        return
        
    if not os.path.exists(usd_file_path):
        unreal.log_error("File does not exist")
        return
    
    project_dir = unreal.Paths.project_dir()
    # Check if assets exist in /Game/USDImport
    if not in_engine_destination:
        usd_import_dir = os.path.join(project_dir, "Content", "USDImport")
        if not os.path.exists(usd_import_dir):
            os.makedirs(usd_import_dir)

    print(in_engine_destination)

    task = unreal.AssetImportTask()
    task.filename = usd_file_path
    task.destination_path = in_engine_destination if in_engine_destination else '/Game/USDImport' # If no destination is selected, import to /Game/USDImport
    task.replace_existing = True
    task.automated = True
    
    options = unreal.UsdStageImportOptions()
    options.render_context_to_import = unreal.Name('unreal')
    options.material_purpose = 'full'
    options.override_stage_options = True
    
    stage_options = unreal.UsdStageOptions()
    stage_options.meters_per_unit = 0.0625
    stage_options.up_axis = unreal.UsdUpAxis.Y_AXIS
    
    options.stage_options = stage_options
    
    options.nanite_triangle_threshold = 1000 # This is something im 50/50 on, Nanite should be on everything but it might be too much for some assets
    
    task.options = options
    task.save = True
    
    asset_tools = unreal.AssetToolsHelpers.get_asset_tools()
    asset_tools.import_asset_tasks([task])
    
if __name__ == "__main__":
    main()