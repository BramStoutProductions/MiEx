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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.Atlas;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.AtlasCreator;

public class AtlasCreatorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public AtlasCreatorDialog() {
		super(MCWorldExporter.getApp().getUI());
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(new EmptyBorder(16, 16, 16, 16));
		add(root);
		
		JLabel resourcePackLabel = new JLabel("Resource Pack");
		root.add(resourcePackLabel);
		
		List<String> resourcePacks = new ArrayList<String>();
		File dir = new File(FileUtil.getResourcePackDir().substring(0, FileUtil.getResourcePackDir().length()-1));
		if(dir.exists()) {
			File[] files = dir.listFiles();
			if(files != null)
				for (File f : files)
					if (f.isDirectory())
						resourcePacks.add(f.getName());
		}
		String[] resourcePacksArray = new String[resourcePacks.size()];
		resourcePacks.toArray(resourcePacksArray);
		JComboBox<String> resourcePackSelector = new JComboBox<String>(resourcePacksArray);
		resourcePackSelector.setEditable(false);
		root.add(resourcePackSelector);
		
		root.add(new JPanel());
		
		JLabel excludeTexturesLabel = new JLabel("Exclude Textures");
		root.add(excludeTexturesLabel);
		JTextArea excludeTexturesCtrl = new JTextArea();
		root.add(excludeTexturesCtrl);
		
		root.add(new JPanel());
		
		JLabel utilityTexturesLabel = new JLabel("Utility Texture Suffixes");
		root.add(utilityTexturesLabel);
		JTextArea utilityTexturesCtrl = new JTextArea("emission\nbump\nnormal\nspecular\nroughness");
		root.add(utilityTexturesCtrl);
		
		root.add(new JPanel());
		
		JButton createButton = new JButton("Create Atlasses");
		root.add(createButton);
		
		
		setSize(400, 600);
		setTitle("Atlas Creator");
		
		createButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				AtlasCreator creator = new AtlasCreator();
				creator.resourcePack = (String) resourcePackSelector.getSelectedItem();
				if(creator.resourcePack == null)
					return;
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
			}
			
		});
	}

}
