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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.ui.ToggleList.SelectionListener;

public class EntityDialog extends JDialog{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ToggleList spawnRules;
	private ToggleList exportEntities;
	private ToggleList simulateEntities;
	private JTextField startFrameInput;
	private JTextField endFrameInput;
	private JTextField fpsInput;
	private JTextField randomSeedInput;
	private JTextField spawnDensityInput;
	private JSpinner sunLightLevelInput;
	
	private JButton doneButton;
	
	public boolean noDefaultSelection = false;
	
	public EntityDialog(Frame owner) {
		super(owner);
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(new EmptyBorder(0, 0, 0, 0));
		add(root);
		
		JPanel listsPanel = new JPanel();
		listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.X_AXIS));
		listsPanel.setBorder(new EmptyBorder(8, 8, 0, 8));
		root.add(listsPanel);

		spawnRules = new ToggleList("Spawn Entities");
		spawnRules.setBorder(new EmptyBorder(8, 8, 8, 8));
		ToolTips.registerTooltip(spawnRules, ToolTips.ENTITY_DIALOG_SPAWN_RULES);
		listsPanel.add(spawnRules);
		
		exportEntities = new ToggleList("Export Entities");
		exportEntities.setBorder(new EmptyBorder(8, 8, 8, 8));
		ToolTips.registerTooltip(exportEntities, ToolTips.ENTITY_DIALOG_EXPORT);
		listsPanel.add(exportEntities);
		
		simulateEntities = new ToggleList("Simulate Entities");
		simulateEntities.setBorder(new EmptyBorder(8, 8, 8, 8));
		ToolTips.registerTooltip(simulateEntities, ToolTips.ENTITY_DIALOG_SIMULATE);
		listsPanel.add(simulateEntities);
		
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.X_AXIS));
		settingsPanel.setMinimumSize(new Dimension(0, 36));
		settingsPanel.setMaximumSize(new Dimension(10000, 36));
		settingsPanel.setBorder(new EmptyBorder(0, 8, 8, 8));
		root.add(settingsPanel);
		
		settingsPanel.add(new JPanel());
		
		JLabel startFrameLabel = new JLabel("Start Frame: ");
		ToolTips.registerTooltip(startFrameLabel, ToolTips.ENTITY_DIALOG_START_FRAME);
		settingsPanel.add(startFrameLabel);
		startFrameInput = new JTextField("0");
		startFrameInput.setMinimumSize(new Dimension(64, 0));
		startFrameInput.setPreferredSize(new Dimension(64, 20));
		startFrameInput.setMaximumSize(new Dimension(64, 20));
		startFrameInput.setHorizontalAlignment(JTextField.RIGHT);
		ToolTips.registerTooltip(startFrameInput, ToolTips.ENTITY_DIALOG_START_FRAME);
		settingsPanel.add(startFrameInput);
		
		settingsPanel.add(new JPanel());
		
		JLabel endFrameLabel = new JLabel("End Frame: ");
		ToolTips.registerTooltip(endFrameLabel, ToolTips.ENTITY_DIALOG_END_FRAME);
		settingsPanel.add(endFrameLabel);
		endFrameInput = new JTextField("1000");
		endFrameInput.setMinimumSize(new Dimension(64, 0));
		endFrameInput.setPreferredSize(new Dimension(64, 20));
		endFrameInput.setMaximumSize(new Dimension(64, 20));
		endFrameInput.setHorizontalAlignment(JTextField.RIGHT);
		ToolTips.registerTooltip(endFrameInput, ToolTips.ENTITY_DIALOG_END_FRAME);
		settingsPanel.add(endFrameInput);
		
		settingsPanel.add(new JPanel());
		
		JLabel fpsLabel = new JLabel("FPS: ");
		ToolTips.registerTooltip(fpsLabel, ToolTips.ENTITY_DIALOG_FPS);
		settingsPanel.add(fpsLabel);
		fpsInput = new JTextField("24");
		fpsInput.setMinimumSize(new Dimension(32, 0));
		fpsInput.setPreferredSize(new Dimension(32, 20));
		fpsInput.setMaximumSize(new Dimension(32, 20));
		fpsInput.setHorizontalAlignment(JTextField.RIGHT);
		ToolTips.registerTooltip(fpsInput, ToolTips.ENTITY_DIALOG_FPS);
		settingsPanel.add(fpsInput);
		
		settingsPanel.add(new JPanel());
		
		JLabel randomSeedLabel = new JLabel("Random Seed: ");
		ToolTips.registerTooltip(randomSeedLabel, ToolTips.ENTITY_DIALOG_RANDOM_SEED);
		settingsPanel.add(randomSeedLabel);
		randomSeedInput = new JTextField("0");
		randomSeedInput.setMinimumSize(new Dimension(64, 0));
		randomSeedInput.setPreferredSize(new Dimension(64, 20));
		randomSeedInput.setMaximumSize(new Dimension(64, 20));
		randomSeedInput.setHorizontalAlignment(JTextField.RIGHT);
		ToolTips.registerTooltip(randomSeedInput, ToolTips.ENTITY_DIALOG_RANDOM_SEED);
		settingsPanel.add(randomSeedInput);
		
		settingsPanel.add(new JPanel());
		
		JLabel spawnDensityLabel = new JLabel("Spawn Density: ");
		ToolTips.registerTooltip(spawnDensityLabel, ToolTips.ENTITY_DIALOG_SPAWN_DENSITY);
		settingsPanel.add(spawnDensityLabel);
		spawnDensityInput = new JTextField("1000");
		spawnDensityInput.setMinimumSize(new Dimension(64, 0));
		spawnDensityInput.setPreferredSize(new Dimension(64, 20));
		spawnDensityInput.setMaximumSize(new Dimension(64, 20));
		spawnDensityInput.setHorizontalAlignment(JTextField.RIGHT);
		ToolTips.registerTooltip(spawnDensityInput, ToolTips.ENTITY_DIALOG_SPAWN_DENSITY);
		settingsPanel.add(spawnDensityInput);
		
		settingsPanel.add(new JPanel());
		
		JLabel sunLightLevelLabel = new JLabel("Sun Light Level: ");
		ToolTips.registerTooltip(sunLightLevelLabel, ToolTips.ENTITY_DIALOG_SUN_LIGHT_LEVEL);
		settingsPanel.add(sunLightLevelLabel);
		sunLightLevelInput = new JSpinner(new SpinnerNumberModel(15, 0, 15, 1));
		sunLightLevelInput.setMinimumSize(new Dimension(48, 0));
		sunLightLevelInput.setPreferredSize(new Dimension(48, 20));
		sunLightLevelInput.setMaximumSize(new Dimension(48, 20));
		ToolTips.registerTooltip(sunLightLevelInput, ToolTips.ENTITY_DIALOG_SUN_LIGHT_LEVEL);
		settingsPanel.add(sunLightLevelInput);
		
		settingsPanel.add(new JPanel());
		
		doneButton = new JButton("Done");
		doneButton.setMinimumSize(new Dimension(64, 0));
		doneButton.setPreferredSize(new Dimension(64, 20));
		doneButton.setMaximumSize(new Dimension(64, 20));
		settingsPanel.add(doneButton);
		
		settingsPanel.add(new JPanel());
		
		setSize(1000, 800);
		setTitle("Entities");
		
		spawnRules.addSelectionListener(new SelectionListener() {

			@Override
			public void onSelectionChanged(List<Entry<String, Boolean>> items) {
				Set<String> entityNamesSet = new HashSet<String>();
				
				entityNamesSet.add("minecraft:item_frame");
				entityNamesSet.add("minecraft:glow_item_frame");
				entityNamesSet.add("minecraft:painting");
				
				if(MCWorldExporter.getApp().getWorld() != null) {
					List<List<Entity>> entities = MCWorldExporter.getApp().getWorld().getEntitiesInRegion(
							MCWorldExporter.getApp().getExportBounds());
					for(List<Entity> entities2 : entities) {
						for(Entity entity : entities2) {
							entityNamesSet.add(entity.getId());
						}
					}
				}
				
				for(Entry<String, Boolean> item : items) {
					if(item.getValue().booleanValue()) {
						entityNamesSet.add(item.getKey());
					}
				}
				
				List<String> entityNames = new ArrayList<String>();
				entityNames.addAll(entityNamesSet);
				entityNames.sort(new Comparator<String>() {

					@Override
					public int compare(String o1, String o2) {
						int o1Pos = o1.indexOf(':');
						int o2Pos = o2.indexOf(':');
						
						if(o1Pos < 0 || o2Pos < 0)
							return o1.compareTo(o2);
						
						String namespace1 = o1.substring(0, o1Pos);
						String namespace2 = o2.substring(0, o2Pos);
						int compare = namespace1.compareTo(namespace2);
						if(compare != 0)
							return compare;
						
						String id1 = o1.substring(o1Pos + 1);
						String id2 = o2.substring(o2Pos + 1);
						return id1.compareTo(id2);
					}
					
				});
				
				exportEntities.setItemsPreserveSelection(entityNames);
			}
			
		});
		
		exportEntities.addSelectionListener(new SelectionListener() {

			@Override
			public void onSelectionChanged(List<Entry<String, Boolean>> items) {
				List<String> entityNames = new ArrayList<String>();
				
				for(Entry<String, Boolean> item : items) {
					if(item.getValue().booleanValue()) {
						entityNames.add(item.getKey());
					}
				}
				
				simulateEntities.setItemsPreserveSelection(entityNames);
			}
			
		});
		
		doneButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
			
		});
	}
	
	public void load() {
		List<EntitySpawner> spawners = ResourcePacks.getEntitySpawners();
		Set<String> spawnerNamesSet = new HashSet<String>();
		for(EntitySpawner spawner : spawners) {
			spawnerNamesSet.add(spawner.getEntityType());
		}
		List<String> spawnerNames = new ArrayList<String>(spawnerNamesSet);
		spawnerNames.sort(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int o1Pos = o1.indexOf(':');
				int o2Pos = o2.indexOf(':');
				
				if(o1Pos < 0 || o2Pos < 0)
					return o1.compareTo(o2);
				
				String namespace1 = o1.substring(0, o1Pos);
				String namespace2 = o2.substring(0, o2Pos);
				int compare = namespace1.compareTo(namespace2);
				if(compare != 0)
					return compare;
				
				String id1 = o1.substring(o1Pos + 1);
				String id2 = o2.substring(o2Pos + 1);
				return id1.compareTo(id2);
			}
			
		});
		
		spawnRules.setItemsPreserveSelection(spawnerNames);
		
		if(!noDefaultSelection)
			exportEntities.setSelection(Arrays.asList("minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:painting"));
		noDefaultSelection = true;
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		
		if(b) {
			SwingUtilities.invokeLater(new Runnable() {
	
				@Override
				public void run() {
					load();
				}
				
			});
		}
	}
	
	public ToggleList getSpawnRules() {
		return spawnRules;
	}
	
	public ToggleList getExportEntities() {
		return exportEntities;
	}
	
	public ToggleList getSimulateEntities() {
		return simulateEntities;
	}
	
	public int getStartFrame() {
		try {
			return Integer.parseInt(startFrameInput.getText());
		}catch(Exception ex) {}
		return 0;
	}
	
	public int getEndFrame() {
		try {
			return Integer.parseInt(endFrameInput.getText());
		}catch(Exception ex) {}
		return 0;
	}
	
	public int getFPS() {
		try {
			return Integer.parseInt(fpsInput.getText());
		}catch(Exception ex) {}
		return 24;
	}
	
	public int getRandomSeed() {
		try {
			return Integer.parseInt(randomSeedInput.getText());
		}catch(Exception ex) {}
		return randomSeedInput.getText().hashCode();
	}
	
	public int getSpawnDensityInput() {
		try {
			return Integer.parseInt(spawnDensityInput.getText());
		}catch(Exception ex) {}
		return 1000;
	}
	
	public int getSunLightLevel() {
		return ((Number) sunLightLevelInput.getValue()).intValue();
	}
	
	public void setStartFrame(int startFrame) {
		startFrameInput.setText(Integer.toString(startFrame));
	}
	
	public void setEndFrame(int endFrame) {
		endFrameInput.setText(Integer.toString(endFrame));
	}
	
	public void setFPS(int fps) {
		fpsInput.setText(Integer.toString(fps));
	}
	
	public void setRandomSeed(int seed) {
		randomSeedInput.setText(Integer.toString(seed));
	}
	
	public void setSpawnDensity(int spawnDensity) {
		spawnDensityInput.setText(Integer.toString(spawnDensity));
	}
	
	public void setSunLightLevel(int sunLightLevel) {
		sunLightLevelInput.setValue(Integer.valueOf(sunLightLevel));
	}

}
