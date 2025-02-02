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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.pbr.PbrGenerator;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.java.ResourcePackJavaEdition;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class PbrGeneratorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JComboBox<String> saveMode;
	private JTextField saveToInput;
	private ResourcePackSelector resourcePackSelector;
	
	public PbrGeneratorDialog() {
		super(MCWorldExporter.getApp().getUI());
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
		root.setBorder(new EmptyBorder(0, 0, 0, 0));
		add(root);
		
		JPanel leftSide = new JPanel();
		leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));
		root.add(leftSide);
		leftSide.setBorder(new EmptyBorder(16, 16, 16, 8));
		
		JLabel utilityTexturesLabel = new JLabel("Utility Texture Suffixes");
		ToolTips.registerTooltip(utilityTexturesLabel, ToolTips.PBR_GENERATOR_DIALOG_UTILITY_TEXTURES);
		leftSide.add(utilityTexturesLabel);
		JTextArea utilityTexturesCtrl = new JTextArea("_emission\n_emissionMask\n_bump\n_normal\n_specular\n_roughness\n_metalness\n_n\n_s");
		ToolTips.registerTooltip(utilityTexturesCtrl, ToolTips.PBR_GENERATOR_DIALOG_UTILITY_TEXTURES);
		leftSide.add(utilityTexturesCtrl);
		
		leftSide.add(new JPanel());
		
		JPanel saveModePanel = new JPanel();
		saveModePanel.setLayout(new BoxLayout(saveModePanel, BoxLayout.X_AXIS));
		saveModePanel.setBorder(new EmptyBorder(0,0,8,0));
		saveModePanel.setPreferredSize(new Dimension(1000, 32));
		saveModePanel.setMaximumSize(new Dimension(1000, 32));
		leftSide.add(saveModePanel);
		
		saveMode = new JComboBox<String>();
		saveMode.addItem("Save Next To Source Texture");
		saveMode.addItem("Save To Separate Resource Pack");
		saveMode.setSelectedIndex(0);
		ToolTips.registerTooltip(saveMode, ToolTips.PBR_GENERATOR_DIALOG_SAVE_MODE);
		saveModePanel.add(saveMode);
		
		JPanel createPanel = new JPanel();
		createPanel.setLayout(new BoxLayout(createPanel, BoxLayout.X_AXIS));
		createPanel.setBorder(new EmptyBorder(0,0,0,0));
		createPanel.setPreferredSize(new Dimension(1000, 24));
		createPanel.setMaximumSize(new Dimension(1000, 24));
		leftSide.add(createPanel);
		
		saveToInput = new JTextField();
		saveToInput.setToolTipText("The resource pack name to save the atlases to.");
		saveToInput.setEnabled(false);
		ToolTips.registerTooltip(saveToInput, ToolTips.PBR_GENERATOR_DIALOG_SAVE_TO);
		createPanel.add(saveToInput);
		
		JButton createButton = new JButton("Create PBR Textures");
		createPanel.add(createButton);
		
		resourcePackSelector = new ResourcePackSelector(false);
		resourcePackSelector.setBorder(new EmptyBorder(16, 8, 16, 16));
		resourcePackSelector.setMaximumSize(new Dimension(396, 1000));
		resourcePackSelector.setPreferredSize(new Dimension(396, 1000));
		root.add(resourcePackSelector);
		resourcePackSelector.reset(false);
		for(int i = ResourcePacks.getActiveResourcePacks().size()-1; i >= 0; --i)
			resourcePackSelector.enableResourcePack(ResourcePacks.getActiveResourcePacks().get(i).getUUID());
		
		setSize(800, 600);
		setTitle("PBR Generator");
		
		saveMode.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				saveToInput.setEnabled(saveMode.getSelectedIndex() == 1);
			}
			
		});
		
		createButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(saveMode.getSelectedIndex() == 1) {
					if(saveToInput.getText().toLowerCase().contains("save to resource pack") || saveToInput.getText().trim().isEmpty()) {
						JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Please specify a name for the resource pack to save the PBR textures to.", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				
				PbrGenerator generator = new PbrGenerator();
				
				for(String rpName : resourcePackSelector.getActiveResourcePacks()) {
					generator.resourcePacks.add(ResourcePacks.getResourcePack(rpName));
				}
				
				if(saveMode.getSelectedIndex() == 1) {
					File rpFolder = new File(FileUtil.getResourcePackDir(), saveToInput.getText());
					rpFolder.mkdirs();
					generator.saveToResourcePack = new ResourcePackJavaEdition(rpFolder);
				}
				
				String utilityTexturesArray[] = utilityTexturesCtrl.getText().split("\\n");
				for(String s : utilityTexturesArray) {
					generator.utilitySuffixes.add(s);
				}
				
				try {
					generator.init();
					generator.process();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "PBR textures created successfully", "Done", JOptionPane.PLAIN_MESSAGE);
				setVisible(false);
				
				// Reload resource packs.
				List<ResourcePack> activeResourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
				List<String> activeResourcePackUUIDS = new ArrayList<String>();
				for(ResourcePack pack : activeResourcePacks)
					activeResourcePackUUIDS.add(pack.getUUID());
				
				ResourcePacks.load();
				ResourcePacks.setActiveResourcePacks(activeResourcePacks);
				
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
				MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(activeResourcePackUUIDS);
				
				Atlas.readAtlasConfig();
				Config.load();
				BlockStateRegistry.clearBlockStateRegistry();
				ModelRegistry.clearModelRegistry();
				BiomeRegistry.recalculateTints();
				MCWorldExporter.getApp().getUI().update();
				MCWorldExporter.getApp().getUI().fullReRender();
			}
			
		});
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(b) {
			resourcePackSelector.reset(false);
			for(int i = ResourcePacks.getActiveResourcePacks().size()-1; i >= 0; --i)
				resourcePackSelector.enableResourcePack(ResourcePacks.getActiveResourcePacks().get(i).getUUID());
			saveToInput.setText("Save To Resource Pack");
			saveToInput.setEnabled(false);
			saveMode.setSelectedIndex(0);
		}
	}

}
