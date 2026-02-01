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

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackDefaults;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class ResourcePackExtractorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ResourcePackExtractorDialog() {
		super(MCWorldExporter.getApp().getUI(), Dialog.ModalityType.APPLICATION_MODAL);
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(new EmptyBorder(16, 16, 16, 16));
		add(root);
		
		JLabel modsFolderLabel = new JLabel("Mods folder");
		root.add(modsFolderLabel);
		JPanel modsFolderPanel = new JPanel();
		modsFolderPanel.setLayout(new BoxLayout(modsFolderPanel, BoxLayout.X_AXIS));
		root.add(modsFolderPanel);
		JTextField modsFolderInput = new JTextField();
		modsFolderPanel.add(modsFolderInput);
		JButton modsFolderBrowseButton = new JButton("...");
		modsFolderPanel.add(modsFolderBrowseButton);
		
		root.add(new JPanel());
		
		JLabel resourcePackLabel = new JLabel("Resource Pack Name");
		root.add(resourcePackLabel);
		JTextField resourcePackInput = new JTextField();
		root.add(resourcePackInput);
		
		root.add(new JPanel());
		
		JButton createButton = new JButton("Extract to Resource Pack");
		root.add(createButton);
		
		
		setSize(400, 180);
		setTitle("Extract Mod Resource Pack");
		
		modsFolderBrowseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setApproveButtonText("Select");
				chooser.setDialogTitle("Select Mods Folder");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (new File(FileUtil.getMinecraftRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getMinecraftRootDir()));
				if (!FileUtil.getMultiMCRootDir().equals("") && new File(FileUtil.getMultiMCRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getMultiMCRootDir()));
				if (!FileUtil.getTechnicRootDir().equals("") && new File(FileUtil.getTechnicRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getTechnicRootDir()));
				chooser.setFileFilter(null);
				chooser.setAcceptAllFileFilterUsed(false);
				int result = chooser.showOpenDialog(MCWorldExporter.getApp().getUI());
				if (result == JFileChooser.APPROVE_OPTION) {
					modsFolderInput.setText(chooser.getSelectedFile().getPath());
				}
			}
			
		});
		
		createButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				File modsFolder = new File(modsFolderInput.getText());
				if(!modsFolder.exists()) {
					JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Mods folder doesn't exist!", "", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if(resourcePackInput.getText().isEmpty()) {
					JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Invalid resource pack name!", "", JOptionPane.ERROR_MESSAGE);
					return;
				}
				System.out.println("Extracting resource pack from mods folder: " + modsFolder.getPath());
				System.out.println("Saving into: " + resourcePackInput.getText());
				MCWorldExporter.getApp().getUI().getProgressBar().setText("Extracting resources");
				File resourcePackFolder = new File(FileUtil.getResourcePackDir(), resourcePackInput.getText());
				File[] mods = modsFolder.listFiles();
				int counter = 0;
				for(File mod : mods) {
					if(!mod.isFile())
						continue;
					if(!mod.getName().endsWith(".jar"))
						continue;
					try {
						ResourcePackDefaults.extractResourcePackFromJar(mod, resourcePackFolder);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					MCWorldExporter.getApp().getUI().getProgressBar().setProgress((((float) counter) / ((float) mods.length)) * 0.9f);
					counter++;
				}
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.9f);
				MCWorldExporter.getApp().getUI().getProgressBar().setText("Infering MiEx config");
				ResourcePackDefaults.inferMiExConfigFromResourcePack(resourcePackFolder);
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(1.0f);
				
				
				System.out.println("Mod resource pack extracted successfully.");
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Mod resource pack extracted successfully!", "Done", JOptionPane.PLAIN_MESSAGE);
				setVisible(false);
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
				MCWorldExporter.getApp().getUI().getProgressBar().setText("");
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset(true);
				
				// Reload everything
				ResourcePacks.load();
				List<ResourcePack> currentlyLoaded = ResourcePacks.getActiveResourcePacks();
				List<String> currentlyLoadedUUIDS = new ArrayList<String>();
				for(ResourcePack pack : currentlyLoaded)
					currentlyLoadedUUIDS.add(pack.getUUID());
				
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
				MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(currentlyLoadedUUIDS);
				
				Atlas.readAtlasConfig();
				Config.load();
				BlockStateRegistry.clearBlockStateRegistry();
				ModelRegistry.clearModelRegistry();
				BiomeRegistry.recalculateTints();
				ResourcePacks.doPostLoad();
				MCWorldExporter.getApp().getUI().update();
				MCWorldExporter.getApp().getUI().fullReRender();
			}
			
		});
	}

}
