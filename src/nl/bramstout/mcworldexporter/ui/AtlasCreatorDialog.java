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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.atlas.AtlasCreator;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

public class AtlasCreatorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JTextField saveToInput;
	
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
		leftSide.add(excludeTexturesLabel);
		JTextArea excludeTexturesCtrl = new JTextArea();
		leftSide.add(excludeTexturesCtrl);
		
		leftSide.add(new JPanel());
		
		JLabel utilityTexturesLabel = new JLabel("Utility Texture Suffixes");
		leftSide.add(utilityTexturesLabel);
		JTextArea utilityTexturesCtrl = new JTextArea("emission\nbump\nnormal\nspecular\nroughness\nn\ns");
		leftSide.add(utilityTexturesCtrl);
		
		leftSide.add(new JPanel());
		
		JPanel createPanel = new JPanel();
		createPanel.setLayout(new BoxLayout(createPanel, BoxLayout.X_AXIS));
		createPanel.setBorder(new EmptyBorder(0,0,0,0));
		createPanel.setPreferredSize(new Dimension(1000, 24));
		createPanel.setMaximumSize(new Dimension(1000, 24));
		leftSide.add(createPanel);
		
		saveToInput = new JTextField();
		saveToInput.setToolTipText("The resource pack name to save the atlases to.");
		createPanel.add(saveToInput);
		
		JButton createButton = new JButton("Create Atlasses");
		createPanel.add(createButton);
		
		ResourcePackSelector resourcePackSelector = new ResourcePackSelector();
		resourcePackSelector.setBorder(new EmptyBorder(16, 8, 16, 16));
		resourcePackSelector.setMaximumSize(new Dimension(324, 1000));
		resourcePackSelector.setPreferredSize(new Dimension(324, 1000));
		root.add(resourcePackSelector);
		resourcePackSelector.reset();
		resourcePackSelector.clear();
		for(int i = ResourcePack.getActiveResourcePacks().size()-1; i >= 0; --i)
			resourcePackSelector.enableResourcePack(ResourcePack.getActiveResourcePacks().get(i));
		
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
				List<String> oldResourcePacks = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
				ResourcePack.setActiveResourcePacks(resourcePackSelector.getActiveResourcePacks());
				
				AtlasCreator creator = new AtlasCreator();
				creator.resourcePack = saveToInput.getText();
				String excludeTexturesArray[] = excludeTexturesCtrl.getText().split("\\n");
				for(String s : excludeTexturesArray) {
					if(!s.startsWith("block/"))
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
				ResourcePack.setActiveResourcePacks(oldResourcePacks);
				
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset();
				MCWorldExporter.getApp().getUI().getResourcePackManager().clear();
				for(int i = oldResourcePacks.size()-1; i >= 0; --i)
					MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(oldResourcePacks.get(i));
			}
			
		});
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(b) {
			saveToInput.setText("Save To Resource Pack");
		}
	}

}
