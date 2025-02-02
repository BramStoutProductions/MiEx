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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.atlas.AtlasCreator;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class AtlasCreatorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JTextField saveToInput;
	private JSpinner repeatsInput;
	private JSpinner paddingInput;
	private ResourcePackSelector resourcePackSelector;
	
	public AtlasCreatorDialog() {
		super(MCWorldExporter.getApp().getUI());
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
		root.setBorder(new EmptyBorder(0, 0, 0, 0));
		add(root);
		
		JPanel leftSide = new JPanel();
		leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));
		root.add(leftSide);
		leftSide.setBorder(new EmptyBorder(16, 16, 16, 8));
		
		JLabel excludeTexturesLabel = new JLabel("Exclude Textures");
		ToolTips.registerTooltip(excludeTexturesLabel, ToolTips.ATLAS_CREATOR_DIALOG_EXCLUDE_TEXTURES);
		leftSide.add(excludeTexturesLabel);
		JTextArea excludeTexturesCtrl = new JTextArea();
		ToolTips.registerTooltip(excludeTexturesCtrl, ToolTips.ATLAS_CREATOR_DIALOG_EXCLUDE_TEXTURES);
		leftSide.add(excludeTexturesCtrl);
		
		leftSide.add(new JPanel());
		
		JLabel utilityTexturesLabel = new JLabel("Utility Texture Suffixes");
		ToolTips.registerTooltip(utilityTexturesLabel, ToolTips.ATLAS_CREATOR_DIALOG_UTILITY_TEXTURES);
		leftSide.add(utilityTexturesLabel);
		JTextArea utilityTexturesCtrl = new JTextArea("_emission\n_emissionMask\n_bump\n_normal\n_specular\n_roughness\n_metalness\n_n\n_s");
		ToolTips.registerTooltip(utilityTexturesCtrl, ToolTips.ATLAS_CREATOR_DIALOG_UTILITY_TEXTURES);
		leftSide.add(utilityTexturesCtrl);
		
		leftSide.add(new JPanel());
		
		JPanel repeatsPanel = new JPanel();
		repeatsPanel.setLayout(new BoxLayout(repeatsPanel, BoxLayout.X_AXIS));
		repeatsPanel.setBorder(new EmptyBorder(0,0,0,0));
		repeatsPanel.setPreferredSize(new Dimension(1000, 24));
		repeatsPanel.setMaximumSize(new Dimension(1000, 24));
		ToolTips.registerTooltip(repeatsPanel, ToolTips.ATLAS_CREATOR_DIALOG_REPEATS);
		leftSide.add(repeatsPanel);
		JLabel repeatsLabel = new JLabel("Repeats");
		repeatsPanel.add(repeatsLabel);
		repeatsPanel.add(new JPanel());
		repeatsInput = new JSpinner(new SpinnerNumberModel(Integer.valueOf(4), 
				Integer.valueOf(1), Integer.valueOf(64), Integer.valueOf(1)));
		repeatsPanel.add(repeatsInput);
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		repeatsPanel.add(new JPanel());
		
		JPanel paddingPanel = new JPanel();
		paddingPanel.setLayout(new BoxLayout(paddingPanel, BoxLayout.X_AXIS));
		paddingPanel.setBorder(new EmptyBorder(0,0,0,0));
		paddingPanel.setPreferredSize(new Dimension(1000, 24));
		paddingPanel.setMaximumSize(new Dimension(1000, 24));
		ToolTips.registerTooltip(paddingPanel, ToolTips.ATLAS_CREATOR_DIALOG_PADDING);
		leftSide.add(paddingPanel);
		JLabel paddingLabel = new JLabel("Padding ");
		paddingPanel.add(paddingLabel);
		paddingPanel.add(new JPanel());
		paddingInput = new JSpinner(new SpinnerNumberModel(Integer.valueOf(1), 
				Integer.valueOf(0), Integer.valueOf(64), Integer.valueOf(1)));
		paddingPanel.add(paddingInput);
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		paddingPanel.add(new JPanel());
		
		leftSide.add(new JPanel());
		
		JPanel createPanel = new JPanel();
		createPanel.setLayout(new BoxLayout(createPanel, BoxLayout.X_AXIS));
		createPanel.setBorder(new EmptyBorder(0,0,0,0));
		createPanel.setPreferredSize(new Dimension(1000, 24));
		createPanel.setMaximumSize(new Dimension(1000, 24));
		leftSide.add(createPanel);
		
		saveToInput = new JTextField();
		saveToInput.setToolTipText("The resource pack name to save the atlases to.");
		ToolTips.registerTooltip(saveToInput, ToolTips.ATLAS_CREATOR_DIALOG_SAVE_TO);
		createPanel.add(saveToInput);
		
		JButton createButton = new JButton("Create Atlasses");
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
		setTitle("Atlas Creator");
		
		createButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(saveToInput.getText().toLowerCase().contains("save to resource pack") || saveToInput.getText().trim().isEmpty()) {
					JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Please specify a name for the resource pack to save the atlases to.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// Set the resource packs
				List<ResourcePack> oldResourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
				List<String> oldResourcePackUUIDS = new ArrayList<String>();
				for(ResourcePack pack : oldResourcePacks)
					oldResourcePackUUIDS.add(pack.getUUID());
				ResourcePacks.setActiveResourcePackUUIDs(resourcePackSelector.getActiveResourcePacks());
				
				AtlasCreator creator = new AtlasCreator();
				creator.resourcePack = saveToInput.getText();
				creator.repeats = ((Integer) repeatsInput.getValue()).intValue();
				creator.padding = ((Integer) paddingInput.getValue()).intValue();
				String excludeTexturesArray[] = excludeTexturesCtrl.getText().split("\\n");
				for(String s : excludeTexturesArray) {
					if(s.isEmpty())
						continue;
					if(!s.startsWith("block/") && !s.startsWith("item/"))
						s = "block/" + s;
					if(!s.contains(":"))
						s = "minecraft:" + s;
					creator.excludeTextures.add(s);
				}
				
				String utilityTexturesArray[] = utilityTexturesCtrl.getText().split("\\n");
				for(String s : utilityTexturesArray) {
					creator.utilityTextures.add(s);
				}
				
				creator.process();
				
				Atlas.readAtlasConfig();
				
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Atlases created successfully", "Done", JOptionPane.PLAIN_MESSAGE);
				setVisible(false);
				
				// Restore resource packs.
				ResourcePacks.load();
				ResourcePacks.setActiveResourcePacks(oldResourcePacks);
				
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
				MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(oldResourcePackUUIDS);
				
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
			repeatsInput.setValue(Integer.valueOf(4));
			paddingInput.setValue(Integer.valueOf(1));
		}
	}

}
