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
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class ResourcePackManager extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ResourcePackSelector resourcePackSelector;
	private JButton reloadButton;
	private JButton updateBaseResourcePackButton;
	private JButton extractModResourcePackButton;
	private JButton createAtlassesButton;
	private AtlasCreatorDialog atlasCreator;
	private ResourcePackExtractorDialog resourcePackExtractor;
	
	public ResourcePackManager() {
		super();
		
		setPreferredSize(new Dimension(316, 800));
		setMinimumSize(new Dimension(316, 128));
		setMaximumSize(new Dimension(316, 100000));

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		setBorder(new EmptyBorder(8, 8, 8, 8));
		
		
		resourcePackSelector = new ResourcePackSelector();
		add(resourcePackSelector);
		
		JPanel separator2 = new JPanel();
		add(separator2);
		
		reloadButton = new JButton("Reload Resource Packs");
		reloadButton.setMinimumSize(new Dimension(300, 32));
		reloadButton.setMaximumSize(new Dimension(300, 32));
		reloadButton.setPreferredSize(new Dimension(300, 32));
		add(reloadButton);
		
		updateBaseResourcePackButton = new JButton("Update Base Resourcepack");
		updateBaseResourcePackButton.setMinimumSize(new Dimension(300, 32));
		updateBaseResourcePackButton.setMaximumSize(new Dimension(300, 32));
		updateBaseResourcePackButton.setPreferredSize(new Dimension(300, 32));
		add(updateBaseResourcePackButton);
		
		extractModResourcePackButton = new JButton("Extract Resourcepack from Modpack");
		extractModResourcePackButton.setMinimumSize(new Dimension(300, 32));
		extractModResourcePackButton.setMaximumSize(new Dimension(300, 32));
		extractModResourcePackButton.setPreferredSize(new Dimension(300, 32));
		add(extractModResourcePackButton);
		
		createAtlassesButton = new JButton("Create Atlasses");
		createAtlassesButton.setMinimumSize(new Dimension(300, 32));
		createAtlassesButton.setMaximumSize(new Dimension(300, 32));
		createAtlassesButton.setPreferredSize(new Dimension(300, 32));
		add(createAtlassesButton);
		
		atlasCreator = new AtlasCreatorDialog();
		resourcePackExtractor = new ResourcePackExtractorDialog();
		
		resourcePackSelector.reset();
		
		resourcePackSelector.addChangeListener(new Runnable() {

			@Override
			public void run() {
				ResourcePack.setActiveResourcePacks(resourcePackSelector.getActiveResourcePacks());
			}
		
		});
		
		reloadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> currentlyLoaded = new ArrayList<String>(ResourcePack.getActiveResourcePacks());
				resourcePackSelector.reset();
				for(int i = currentlyLoaded.size()-1; i >= 0; --i)
					resourcePackSelector.enableResourcePack(currentlyLoaded.get(i));
				
				Atlas.readAtlasConfig();
				Config.load();
				BlockStateRegistry.clearBlockStateRegistry();
				ModelRegistry.clearModelRegistry();
				BiomeRegistry.recalculateTints();
				MCWorldExporter.getApp().getUI().update();
				MCWorldExporter.getApp().getUI().fullReRender();
			}
			
		});
		
		updateBaseResourcePackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ResourcePack.updateBaseResourcePack(false);
			}
			
		});
		
		extractModResourcePackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				resourcePackExtractor.setVisible(true);
				resourcePackExtractor.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
			}
			
		});
		
		createAtlassesButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				atlasCreator.setVisible(true);
				atlasCreator.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
			}
			
		});
	}
	
	public void reset() {
		resourcePackSelector.reset();
	}
	
	public void clear() {
		resourcePackSelector.clear();
	}
	
	public void enableResourcePack(String resourcePack) {
		resourcePackSelector.enableResourcePack(resourcePack);
	}
	
	public void disableResourcePack(String resourcePack) {
		resourcePackSelector.disableResourcePack(resourcePack);
	}
	
}
