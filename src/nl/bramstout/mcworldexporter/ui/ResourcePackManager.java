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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalIconFactory;

import nl.bramstout.mcworldexporter.BuiltInFiles;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Preset;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.parallel.BackgroundThread;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackDefaults;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class ResourcePackManager extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ResourcePackSelector resourcePackSelector;
	private JButton presetsButton;
	private JButton toolsButton;
	private JPopupMenu toolsMenu;
	private JMenuItem reloadTool;
	private JMenuItem updateBaseResourcePackTool;
	private JMenuItem updateBaseResourcePackHytaleTool;
	private JMenuItem updateBuiltInFilesTool;
	private JMenuItem exampleResourcePackDownloaderTool;
	private JMenuItem extractModResourcePackTool;
	private JMenuItem createAtlassesTool;
	private JMenuItem pbrGeneratorTool;
	private JMenuItem environmentSettingsTool;
	private AtlasCreatorDialog atlasCreator;
	private ResourcePackExtractorDialog resourcePackExtractor;
	private PbrGeneratorDialog pbrGenerator;
	private ExampleResourcePackDownloader exampleResourcePackDownloader;
	private SavePresetDialog savePresetDialog;
	private EnvironmentSettingsDialog environmentSettings;
	
	public ResourcePackManager() {
		super();
		
		setPreferredSize(new Dimension(316, 800));
		setMinimumSize(new Dimension(316, 128));
		setMaximumSize(new Dimension(316, 100000));

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		setBorder(new EmptyBorder(0, 8, 8, 8));
		
		presetsButton = new JButton("Presets");
		presetsButton.setMinimumSize(new Dimension(300, 24));
		presetsButton.setMaximumSize(new Dimension(300, 24));
		presetsButton.setPreferredSize(new Dimension(300, 24));
		presetsButton.setAlignmentX(0);
		add(presetsButton);
		
		JPanel separator3 = new JPanel();
		separator3.setMinimumSize(new Dimension(30, 8));
		separator3.setMaximumSize(new Dimension(30, 8));
		separator3.setPreferredSize(new Dimension(30, 8));
		separator3.setAlignmentX(0);
		add(separator3);
		
		resourcePackSelector = new ResourcePackSelector(true);
		resourcePackSelector.setAlignmentX(0);
		add(resourcePackSelector);
		
		JPanel separator2 = new JPanel();
		separator2.setAlignmentX(0);
		add(separator2);
		
		toolsButton = new JButton("Tools");
		toolsButton.setMinimumSize(new Dimension(300, 32));
		toolsButton.setMaximumSize(new Dimension(300, 32));
		toolsButton.setPreferredSize(new Dimension(300, 32));
		toolsButton.setAlignmentX(0);
		add(toolsButton);
		
		toolsMenu = new JPopupMenu("Tools");
		reloadTool = toolsMenu.add("Reload Resource Packs");
		ToolTips.registerTooltip(reloadTool, ToolTips.TOOL_RELOAD);
		updateBaseResourcePackTool = toolsMenu.add("Update Base Resourcepack");
		ToolTips.registerTooltip(updateBaseResourcePackTool, ToolTips.TOOL_UPDATE_BASE_RESOURCE_PACK);
		updateBaseResourcePackHytaleTool = toolsMenu.add("Update Base Resourcepack Hytale");
		ToolTips.registerTooltip(updateBaseResourcePackHytaleTool, ToolTips.TOOL_UPDATE_BASE_RESOURCE_PACK_HYTALE);
		updateBuiltInFilesTool = toolsMenu.add("Update Built In Files");
		ToolTips.registerTooltip(updateBuiltInFilesTool, ToolTips.TOOL_UPDATE_BUILT_IN_FILES);
		exampleResourcePackDownloaderTool = toolsMenu.add("Download Example Resourcepacks");
		ToolTips.registerTooltip(exampleResourcePackDownloaderTool, ToolTips.TOOL_DOWNLOAD_EXAMPLE_RESOURCE_PACKS);
		extractModResourcePackTool = toolsMenu.add("Extract Resourcepack from Modpack");
		ToolTips.registerTooltip(extractModResourcePackTool, ToolTips.TOOL_EXTRACT_MOD_RESOURCE_PACK);
		createAtlassesTool = toolsMenu.add("Create Atlasses");
		ToolTips.registerTooltip(createAtlassesTool, ToolTips.TOOL_CREATE_ATLASSES);
		pbrGeneratorTool = toolsMenu.add("Generate PBR textures");
		ToolTips.registerTooltip(pbrGeneratorTool, ToolTips.TOOL_GENERATE_PBR_TEXTURES);
		environmentSettingsTool = toolsMenu.add("Edit Environment Settings");
		ToolTips.registerTooltip(environmentSettingsTool, ToolTips.TOOL_ENVIRONMENT_SETTINGS);
		
		atlasCreator = new AtlasCreatorDialog();
		resourcePackExtractor = new ResourcePackExtractorDialog();
		pbrGenerator = new PbrGeneratorDialog();
		exampleResourcePackDownloader = new ExampleResourcePackDownloader();
		savePresetDialog = new SavePresetDialog();
		environmentSettings = new EnvironmentSettingsDialog();
		
		resourcePackSelector.reset(true);
		
		resourcePackSelector.addChangeListener(new Runnable() {

			@Override
			public void run() {
				BackgroundThread.runInBackground(new Runnable() {

					@Override
					public void run() {
						ResourcePacks.setActiveResourcePackUUIDs(resourcePackSelector.getActiveResourcePacks());
						repaint();
					}
				
				});
			}
		
		});
		
		presetsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JPopupMenu presetsMenu = new JPopupMenu();
				
				JMenuItem saveItem = presetsMenu.add(new JMenuItem("Save Preset", MetalIconFactory.getTreeFloppyDriveIcon()));
				
				saveItem.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						savePresetDialog.setVisible(true);
						savePresetDialog.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
					}
					
				});
				
				for(Preset preset : Preset.getPresets()) {
					JMenuItem presetItem = presetsMenu.add(preset.getName());
					final Preset finalPreset = preset;
					presetItem.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							finalPreset.apply();
						}
						
					});
				}
				
				presetsMenu.show(presetsButton, 0, presetsButton.getHeight());
			}
			
		});
		
		toolsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				toolsMenu.show(toolsButton, 0, toolsButton.getHeight());
			}
			
		});
		
		reloadTool.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				BackgroundThread.runInBackground(new Runnable() {

					@Override
					public void run() {
						ResourcePacks.load();
						List<ResourcePack> currentlyLoaded = ResourcePacks.getActiveResourcePacks();
						List<String> currentlyLoadedUUIDS = new ArrayList<String>();
						for(ResourcePack pack : currentlyLoaded)
							currentlyLoadedUUIDS.add(pack.getUUID());
						
						resourcePackSelector.reset(false);
						resourcePackSelector.enableResourcePack(currentlyLoadedUUIDS);
						
						Atlas.readAtlasConfig();
						Config.load();
						BlockStateRegistry.clearBlockStateRegistry();
						ModelRegistry.clearModelRegistry();
						BiomeRegistry.recalculateTints();
						ResourcePacks.doPostLoad();
						MCWorldExporter.getApp().getUI().update();
						MCWorldExporter.getApp().getUI().fullReRender();
						repaint();
					}
				});
			}
			
		});
		
		updateBaseResourcePackTool.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResourcePackDefaults.updateBaseResourcePack(false);
				
				// Reload everything
				ResourcePacks.load();
				List<ResourcePack> currentlyLoaded = ResourcePacks.getActiveResourcePacks();
				List<String> currentlyLoadedUUIDS = new ArrayList<String>();
				for(ResourcePack pack : currentlyLoaded)
					currentlyLoadedUUIDS.add(pack.getUUID());
				
				resourcePackSelector.reset(false);
				resourcePackSelector.enableResourcePack(currentlyLoadedUUIDS);
				
				Atlas.readAtlasConfig();
				Config.load();
				BlockStateRegistry.clearBlockStateRegistry();
				ModelRegistry.clearModelRegistry();
				BiomeRegistry.recalculateTints();
				ResourcePacks.doPostLoad();
				MCWorldExporter.getApp().getUI().update();
				MCWorldExporter.getApp().getUI().fullReRender();
				repaint();
			}
			
		});
		
		updateBaseResourcePackHytaleTool.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResourcePackDefaults.updateBaseResourcePackHytale(false);
				
				// Reload everything
				ResourcePacks.load();
				List<ResourcePack> currentlyLoaded = ResourcePacks.getActiveResourcePacks();
				List<String> currentlyLoadedUUIDS = new ArrayList<String>();
				for(ResourcePack pack : currentlyLoaded)
					currentlyLoadedUUIDS.add(pack.getUUID());
				
				resourcePackSelector.reset(false);
				resourcePackSelector.enableResourcePack(currentlyLoadedUUIDS);
				
				Atlas.readAtlasConfig();
				Config.load();
				BlockStateRegistry.clearBlockStateRegistry();
				ModelRegistry.clearModelRegistry();
				BiomeRegistry.recalculateTints();
				ResourcePacks.doPostLoad();
				MCWorldExporter.getApp().getUI().update();
				MCWorldExporter.getApp().getUI().fullReRender();
				repaint();
			}
			
		});
		
		updateBuiltInFilesTool.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				BackgroundThread.runInBackground(new Runnable() {

					@Override
					public void run() {
						MCWorldExporter.getApp().getUI().setEnabled(false);
						MCWorldExporter.getApp().getUI().getProgressBar().setText("Updating Built In Files");
						try {
							BuiltInFiles.setupBuiltInFiles(true);
						}catch(Exception ex) {
							ex.printStackTrace();
						}
						MCWorldExporter.getApp().getUI().getProgressBar().setText("");
						MCWorldExporter.getApp().getUI().setEnabled(true);
						
						ResourcePacks.load();
						List<ResourcePack> currentlyLoaded = ResourcePacks.getActiveResourcePacks();
						List<String> currentlyLoadedUUIDS = new ArrayList<String>();
						for(ResourcePack pack : currentlyLoaded)
							currentlyLoadedUUIDS.add(pack.getUUID());
						
						resourcePackSelector.reset(false);
						resourcePackSelector.enableResourcePack(currentlyLoadedUUIDS);
						
						Atlas.readAtlasConfig();
						Config.load();
						BlockStateRegistry.clearBlockStateRegistry();
						ModelRegistry.clearModelRegistry();
						BiomeRegistry.recalculateTints();
						ResourcePacks.doPostLoad();
						MCWorldExporter.getApp().getUI().update();
						MCWorldExporter.getApp().getUI().fullReRender();
						repaint();
						
						JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Built in files successfully installed", "Done", JOptionPane.PLAIN_MESSAGE);
					}
				
				});
			}
			
		});
		
		exampleResourcePackDownloaderTool.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				exampleResourcePackDownloader.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				exampleResourcePackDownloader.setVisible(true);
			}
			
		});
		
		extractModResourcePackTool.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				resourcePackExtractor.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				resourcePackExtractor.setVisible(true);
			}
			
		});
		
		createAtlassesTool.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				atlasCreator.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				atlasCreator.setVisible(true);
			}
			
		});
		
		pbrGeneratorTool.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				pbrGenerator.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				pbrGenerator.setVisible(true);
			}
			
		});
		
		environmentSettingsTool.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				environmentSettings.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				environmentSettings.setVisible(true);
			}
			
		});
	}
	
	public void reset(boolean loadDefaultResourcePacks) {
		resourcePackSelector.reset(loadDefaultResourcePacks);
	}
	
	public void clear() {
		resourcePackSelector.clear();
	}
	
	public void enableResourcePack(String uuid) {
		resourcePackSelector.enableResourcePack(uuid);
	}
	
	public void enableResourcePack(List<String> uuids) {
		resourcePackSelector.enableResourcePack(uuids);
	}
	
	public void disableResourcePack(String uuid) {
		resourcePackSelector.disableResourcePack(uuid);
	}
	
	public void disableResourcePack(List<String> uuids) {
		resourcePackSelector.disableResourcePack(uuids);
	}
	
	public void syncWithResourcePacks() {
		resourcePackSelector.syncWithResourcePacks();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		resourcePackSelector.setEnabled(enabled);
		presetsButton.setEnabled(enabled);
		toolsButton.setEnabled(enabled);
	}
	
}
