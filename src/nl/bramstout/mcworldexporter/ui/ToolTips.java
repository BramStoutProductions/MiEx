/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter.ui;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

public class ToolTips {
	
	private static Map<Component, Object> TOOLTIP_REGISTRY = new HashMap<Component, Object>();
	
	public static interface DynamicTooltip{
		
		public String getTooltip();
		
	}
	
	public static void registerTooltip(Component component, String tooltip) {
		TOOLTIP_REGISTRY.put(component, tooltip);
	}
	
	public static void registerDynamicTooltip(Component component, DynamicTooltip tooltip) {
		TOOLTIP_REGISTRY.put(component, tooltip);
	}
	
	public static String getTooltip(Component component) {
		Object tooltip = TOOLTIP_REGISTRY.getOrDefault(component, null);
		if(tooltip == null) {
			Component parentComponent = component.getParent();
			if(parentComponent == null)
				return "";
			return getTooltip(parentComponent);
		}
		if(tooltip instanceof String)
			return (String) tooltip;
		if(tooltip instanceof DynamicTooltip)
			return ((DynamicTooltip) tooltip).getTooltip();
		return "";
	}
	
	public static String LOAD_WORLD_FROM_EXPORT = "Open up the world and recover the export settings used to create the selected export.";
	public static String LOAD_SETTINGS_FROM_EXPORT = "Loads in the settings from a previous export, without changing the world.";
	public static String PAUSE_LOADING = "Pause or unpause the loading of a world, so that another program can safely open it up.";
	public static String DIMENSION_CHOOSER = "Which dimension to load in.";
	public static String EXPORT_BOUNDS = "The bounds of the area that should be exported.";
	public static String Y_OFFSET = "The Y block position that should be at the origin. Can either manually be specified or automatically calculated. A red border means the value is being updated.";
	public static String LOD = "Specifies the area that should be full quality. Parts outside of it will be progressively lower resolution.";
	public static String LOD_ENABLE = "Enables or disables Level-of-Detail.";
	public static String LOD_Y_DETAIL = "The higher the value, the slower the Y resolution lowers outside of the full quality LOD area.";
	public static String TELEPORT = "Teleport the view to a specific coordinate or player.";
	public static String ZOOM_IN = "Zoom the view in.";
	public static String ZOOM_OUT = "Zoom the view out.";
	public static String RUN_OPTIMISERS = "Run the optimisers available in MiEx and enabled in the config during export.";
	public static String REMOVE_CAVES = "Run the cave removal algorithm during export.";
	public static String REMOVE_CAVES_FILL_IN = "Fill in the holes in the world that get generated by the cave removal algorithm.";
	public static String EXPORT_INDIVIDUAL_BLOCKS = "Export every single block out as individual objects rather than combined into larger meshes. SHOULD ONLY BE USED FOR SMALL EXPORTS!";
	public static String CHUNK_SIZE = "Change the size of the export chunks (measured in Minecraft chunks).";
	public static String EDIT_FG = "Toggle the edit mode for selecting which export chunks should be tagged as foreground chunks.";
	public static String ENTITY_DIALOG = "Open the Entity dialog to specify which entities you want to export.";
	public static String EXPORT = "Export out the selected export region (opens up a dialog to specify where to export to).";
	public static String REEXPORT = "Re-export out the selected export region, overwriting the previous export loaded in.";
	
	public static String ENTITY_DIALOG_SPAWN_RULES = "Select which entities MiEx should spawn, in addition to the entities already in the world.";
	public static String ENTITY_DIALOG_EXPORT = "Select which entities should be exported.";
	public static String ENTITY_DIALOG_SIMULATE = "Select which entities MiEx should simulate and animate.";
	public static String ENTITY_DIALOG_START_FRAME = "The frame number for the start of the animated entities.";
	public static String ENTITY_DIALOG_END_FRAME = "The frame number for the end of the animated entities. They will be simulated until this frame.";
	public static String ENTITY_DIALOG_FPS = "The framerate of the animation.";
	public static String ENTITY_DIALOG_RANDOM_SEED = "The seed used for the random number generation. The same seed provides the same animation every time.";
	public static String ENTITY_DIALOG_SPAWN_DENSITY = "How many times it tries to spawn an entity per given volume. Higher means more entities.";
	public static String ENTITY_DIALOG_SUN_LIGHT_LEVEL = "The light level in the world from the sun. Used to determine what kind of entities can spawn.";
	
	public static String ATLAS_CREATOR_DIALOG_EXCLUDE_TEXTURES = "The resource identifiers of textures to not include in the atlas.";
	public static String ATLAS_CREATOR_DIALOG_UTILITY_TEXTURES = "The suffixes of utility textures. These will be put into their own atlasses with the same layout as the main atlasses.";
	public static String ATLAS_CREATOR_DIALOG_SAVE_TO = "The name of the resource pack that you want to save the atlasses to. Will create it if it doesn't exist yet.";
	public static String ATLAS_CREATOR_DIALOG_REPEATS = "The number of times a texture should be repeated. Needed for LoD and Face optimiser to work with atlases.";
	public static String ATLAS_CREATOR_DIALOG_PADDING = "The number of additional repeats of a texture for padding. Useful when texture sampling can occur outside of the intended UV coordinates.";
	
	public static String TOOL_RELOAD = "Reload the currently active resource packs.";
	public static String TOOL_UPDATE_BASE_RESOURCE_PACK = "Update the base resource pack with the one from a different Minecraft version.";
	public static String TOOL_UPDATE_BUILT_IN_FILES = "Extracts the latest built in files.";
	public static String TOOL_DOWNLOAD_EXAMPLE_RESOURCE_PACKS = "Download or update example resource packs from the GitHub repository.";
	public static String TOOL_EXTRACT_MOD_RESOURCE_PACK = "Extract the built-in resource packs from all mods in the selected mods folder. Useful for when you want to load in a modded world.";
	public static String TOOL_CREATE_ATLASSES = "Opens up the Atlas Creator tool.";
	public static String TOOL_GENERATE_PBR_TEXTURES = "Opens up the PBR Generator tool.";
	public static String TOOL_ENVIRONMENT_SETTINGS = "Opens up the Environment Settings dialog to allow you to change MiEx's environment variables.";
	
	public static String PBR_GENERATOR_DIALOG_UTILITY_TEXTURES = "The suffixes of utility textures. These will be ignored by the PBR Generator.";
	public static String PBR_GENERATOR_DIALOG_SAVE_TO = "The name of the resource pack that you want to save the PBR textures to. Will create it if it doesn't exist yet.";
	public static String PBR_GENERATOR_DIALOG_SAVE_MODE = "Where to save the generated textures. It can save it next to the original texture used or into a separate resource pack.";
	
}
