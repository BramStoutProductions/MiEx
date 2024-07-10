import unreal
from unreal import UsdAssetImportData
import os
import tkinter as tk
from tkinter import filedialog

def select_usd_file():
    root = tk.Tk()
    root.withdraw()  # Hide the main window
    file_path = filedialog.askopenfilename(filetypes=[("USD files", "*.usd")])
    return file_path

def main():

    usd_file_path = select_usd_file()

    if not usd_file_path:
        unreal.log_error("No file selected")
        return
        
    if not os.path.exists(usd_file_path):
        unreal.log_error("File does not exist")
        return
    
    data = UsdAssetImportData() # https://github.com/EpicGames/UnrealEngine/blob/c830445187784f1269f43b56f095493a27d5a636/Engine/Plugins/Importers/USDImporter/Source/USDStageImporter/Private/USDStageImportOptions.cpp
    data.set_editor_property('source_path', usd_file_path)
    data.set_editor_property('destination_path', '/Game/USD/')
    data.set_editor_property('destination_name', os.path.basename(usd_file_path).split('.')[0])
    data.set_editor_property('automated', True)
    data.set_editor_property('RenderContextToImport',0)