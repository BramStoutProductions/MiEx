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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.MCWorldExporter;

public class AdvancedSettingsPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ExportRegionsManager exportRegionsManager;
	private ExcludeRegionsManager excludeRegionsManager;
	private JCheckBox lodEnableCheckBox;
	private JSpinner lodCenterXSpinner;
	private JSpinner lodCenterZSpinner;
	private JSpinner lodWidthSpinner;
	private JSpinner lodDepthSpinner;
	private JSpinner lodYDetailSpinner;
	private JSpinner lodBaseLevelSpinner;
	
	private JCheckBox runOptimiserCheckBox;
	private JCheckBox removeCavesCheckBox;
	private JCheckBox fillInCavesCheckBox;
	private JCheckBox exportIndividualBlocksCheckBox;
	private JCheckBox exportBlockAnimationsCheckBox;
	private JCheckBox excludeRegionsAsAirCheckBox;
	private JCheckBox exportRegionActAsExcludeRegion;
	
	private JButton entityButton;
	
	public AdvancedSettingsPanel() {
		setPreferredSize(new Dimension(600, 800));
		setMinimumSize(new Dimension(600, 128));
		setMaximumSize(new Dimension(600, 100000));

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		setBorder(new EmptyBorder(0, 0, 0, 0));
		
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.setPreferredSize(new Dimension(300, 800));
		leftPanel.setMinimumSize(new Dimension(300, 128));
		leftPanel.setMinimumSize(new Dimension(300, 100000));
		leftPanel.setBorder(new EmptyBorder(0, 8, 8, 4));
		add(leftPanel);
		
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.setPreferredSize(new Dimension(300, 800));
		rightPanel.setMinimumSize(new Dimension(300, 128));
		rightPanel.setMinimumSize(new Dimension(300, 100000));
		rightPanel.setBorder(new EmptyBorder(0, 4, 8, 8));
		add(rightPanel);
		
		this.exportRegionsManager = new ExportRegionsManager();
		rightPanel.add(exportRegionsManager);
		
		this.excludeRegionsManager = new ExcludeRegionsManager();
		this.excludeRegionsManager.setBorder(new EmptyBorder(8, 0, 0, 0));
		rightPanel.add(excludeRegionsManager);
		
		
		JPanel lodPanel = new JPanel();
		lodPanel.setLayout(new BoxLayout(lodPanel, BoxLayout.Y_AXIS));
		lodPanel.setMinimumSize(new Dimension(288, 96));
		lodPanel.setMaximumSize(lodPanel.getMinimumSize());
		lodPanel.setPreferredSize(lodPanel.getMinimumSize());
		ToolTips.registerTooltip(lodPanel, ToolTips.LOD);
		lodEnableCheckBox = new JCheckBox("LOD");
		lodEnableCheckBox.setSelected(false);
		lodEnableCheckBox.setAlignmentX(0.35f);
		ToolTips.registerTooltip(lodEnableCheckBox, ToolTips.LOD_ENABLE);
		lodPanel.add(lodEnableCheckBox);

		JPanel lodCtrlPanel = new JPanel();
		lodCtrlPanel.setLayout(new BoxLayout(lodCtrlPanel, BoxLayout.X_AXIS));

		JPanel lodLabelPanel = new JPanel();
		lodLabelPanel.setLayout(new BoxLayout(lodLabelPanel, BoxLayout.Y_AXIS));
		lodLabelPanel.setPreferredSize(new Dimension(70, 60));
		lodLabelPanel.setMinimumSize(lodLabelPanel.getPreferredSize());
		lodLabelPanel.setMaximumSize(lodLabelPanel.getPreferredSize());
		JLabel lodXLabel = new JLabel("X:");
		lodXLabel.setPreferredSize(new Dimension(70, 20));
		lodXLabel.setMinimumSize(lodXLabel.getPreferredSize());
		lodXLabel.setMaximumSize(lodXLabel.getPreferredSize());
		lodLabelPanel.add(lodXLabel);
		JLabel lodZLabel = new JLabel("Z:");
		lodZLabel.setPreferredSize(new Dimension(70, 20));
		lodZLabel.setMinimumSize(lodZLabel.getPreferredSize());
		lodZLabel.setMaximumSize(lodZLabel.getPreferredSize());
		lodLabelPanel.add(lodZLabel);
		JLabel lodYDetailLabel = new JLabel("Y Detail:");
		lodYDetailLabel.setPreferredSize(new Dimension(70, 20));
		lodYDetailLabel.setMinimumSize(lodYDetailLabel.getPreferredSize());
		lodYDetailLabel.setMaximumSize(lodYDetailLabel.getPreferredSize());
		ToolTips.registerTooltip(lodYDetailLabel, ToolTips.LOD_Y_DETAIL);
		lodLabelPanel.add(lodYDetailLabel);
		lodCtrlPanel.add(lodLabelPanel);

		JPanel lodSpinnerPanel = new JPanel();
		lodSpinnerPanel.setLayout(new BoxLayout(lodSpinnerPanel, BoxLayout.Y_AXIS));
		lodSpinnerPanel.setPreferredSize(new Dimension(70, 60));
		lodSpinnerPanel.setMinimumSize(lodSpinnerPanel.getPreferredSize());
		lodSpinnerPanel.setMaximumSize(lodSpinnerPanel.getPreferredSize());
		lodCenterXSpinner = new JSpinner();
		lodCenterXSpinner.setPreferredSize(new Dimension(70, 20));
		lodCenterXSpinner.setMinimumSize(lodCenterXSpinner.getPreferredSize());
		lodCenterXSpinner.setMaximumSize(lodCenterXSpinner.getPreferredSize());
		lodSpinnerPanel.add(lodCenterXSpinner);
		lodCenterZSpinner = new JSpinner();
		lodCenterZSpinner.setPreferredSize(new Dimension(70, 20));
		lodCenterZSpinner.setMinimumSize(lodCenterZSpinner.getPreferredSize());
		lodCenterZSpinner.setMaximumSize(lodCenterZSpinner.getPreferredSize());
		lodSpinnerPanel.add(lodCenterZSpinner);
		lodYDetailSpinner = new JSpinner();
		lodYDetailSpinner.setPreferredSize(new Dimension(70, 20));
		lodYDetailSpinner.setValue(4);
		((SpinnerNumberModel)(lodYDetailSpinner.getModel())).setMinimum(1);
		((SpinnerNumberModel)(lodYDetailSpinner.getModel())).setMaximum(16);
		lodYDetailSpinner.setMinimumSize(lodYDetailSpinner.getPreferredSize());
		lodYDetailSpinner.setMaximumSize(lodYDetailSpinner.getPreferredSize());
		ToolTips.registerTooltip(lodYDetailSpinner, ToolTips.LOD_Y_DETAIL);
		lodSpinnerPanel.add(lodYDetailSpinner);
		lodCtrlPanel.add(lodSpinnerPanel);
		
		JPanel lodLabel2Panel = new JPanel();
		lodLabel2Panel.setLayout(new BoxLayout(lodLabel2Panel, BoxLayout.Y_AXIS));
		lodLabel2Panel.setPreferredSize(new Dimension(70, 60));
		lodLabel2Panel.setMinimumSize(lodLabel2Panel.getPreferredSize());
		lodLabel2Panel.setMaximumSize(lodLabel2Panel.getPreferredSize());
		JLabel lodWidthLabel = new JLabel("   Width:");
		lodWidthLabel.setPreferredSize(new Dimension(70, 20));
		lodWidthLabel.setMinimumSize(lodWidthLabel.getPreferredSize());
		lodWidthLabel.setMaximumSize(lodWidthLabel.getPreferredSize());
		lodLabel2Panel.add(lodWidthLabel);
		JLabel lodDepthLabel = new JLabel("   Depth:");
		lodDepthLabel.setPreferredSize(new Dimension(70, 20));
		lodDepthLabel.setMinimumSize(lodDepthLabel.getPreferredSize());
		lodDepthLabel.setMaximumSize(lodDepthLabel.getPreferredSize());
		lodLabel2Panel.add(lodDepthLabel);
		JLabel lodBaseLevelLabel = new JLabel("   Base Level:");
		ToolTips.registerTooltip(lodBaseLevelLabel, ToolTips.LOD_BASE_LEVEL);
		lodBaseLevelLabel.setPreferredSize(new Dimension(70, 20));
		lodBaseLevelLabel.setMinimumSize(lodBaseLevelLabel.getPreferredSize());
		lodBaseLevelLabel.setMaximumSize(lodBaseLevelLabel.getPreferredSize());
		lodLabel2Panel.add(lodBaseLevelLabel);
		lodCtrlPanel.add(lodLabel2Panel);
		
		JPanel lodSpinner2Panel = new JPanel();
		lodSpinner2Panel.setLayout(new BoxLayout(lodSpinner2Panel, BoxLayout.Y_AXIS));
		lodSpinner2Panel.setPreferredSize(new Dimension(70, 60));
		lodSpinner2Panel.setMinimumSize(lodSpinner2Panel.getPreferredSize());
		lodSpinner2Panel.setMaximumSize(lodSpinner2Panel.getPreferredSize());
		lodWidthSpinner = new JSpinner();
		lodWidthSpinner.setPreferredSize(new Dimension(70, 20));
		lodWidthSpinner.setMinimumSize(lodWidthSpinner.getPreferredSize());
		lodWidthSpinner.setMaximumSize(lodWidthSpinner.getPreferredSize());
		lodSpinner2Panel.add(lodWidthSpinner);
		lodDepthSpinner = new JSpinner();
		lodDepthSpinner.setPreferredSize(new Dimension(70, 20));
		lodDepthSpinner.setMinimumSize(lodDepthSpinner.getPreferredSize());
		lodDepthSpinner.setMaximumSize(lodDepthSpinner.getPreferredSize());
		lodSpinner2Panel.add(lodDepthSpinner);
		lodBaseLevelSpinner = new JSpinner();
		ToolTips.registerTooltip(lodBaseLevelSpinner, ToolTips.LOD_BASE_LEVEL);
		lodBaseLevelSpinner.setPreferredSize(new Dimension(70, 20));
		lodBaseLevelSpinner.setMinimumSize(lodBaseLevelSpinner.getPreferredSize());
		lodBaseLevelSpinner.setMaximumSize(lodBaseLevelSpinner.getPreferredSize());
		lodSpinner2Panel.add(lodBaseLevelSpinner);
		lodCtrlPanel.add(lodSpinner2Panel);
		lodPanel.add(lodCtrlPanel);
		lodPanel.setAlignmentX(0);
		
		leftPanel.add(lodPanel);
		
		
		JPanel settingsAndEntitiesPanel = new JPanel();
		settingsAndEntitiesPanel.setMinimumSize(new Dimension(288, 140));
		settingsAndEntitiesPanel.setMaximumSize(settingsAndEntitiesPanel.getMinimumSize());
		settingsAndEntitiesPanel.setPreferredSize(settingsAndEntitiesPanel.getMinimumSize());
		settingsAndEntitiesPanel.setLayout(new BoxLayout(settingsAndEntitiesPanel, BoxLayout.X_AXIS));
		settingsAndEntitiesPanel.setAlignmentX(0);
		leftPanel.add(settingsAndEntitiesPanel);
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setMinimumSize(new Dimension(144, 140));
		settingsPanel.setMaximumSize(settingsPanel.getMinimumSize());
		settingsPanel.setPreferredSize(settingsPanel.getMinimumSize());
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		runOptimiserCheckBox = new JCheckBox("Run Optimisers");
		runOptimiserCheckBox.setSelected(Config.runOptimiser);
		runOptimiserCheckBox.setBorder(new EmptyBorder(4, 0, 4, 0));
		ToolTips.registerTooltip(runOptimiserCheckBox, ToolTips.RUN_OPTIMISERS);
		settingsPanel.add(runOptimiserCheckBox);
		runOptimiserCheckBox.setAlignmentX(0);
		
		removeCavesCheckBox = new JCheckBox("Remove Caves");
		removeCavesCheckBox.setSelected(Config.removeCaves);
		removeCavesCheckBox.setBorder(new EmptyBorder(4, 0, 4, 0));
		ToolTips.registerTooltip(removeCavesCheckBox, ToolTips.REMOVE_CAVES);
		settingsPanel.add(removeCavesCheckBox);
		removeCavesCheckBox.setAlignmentX(0);
		
		fillInCavesCheckBox = new JCheckBox("Fill In Caves");
		fillInCavesCheckBox.setSelected(Config.fillInCaves);
		fillInCavesCheckBox.setBorder(new EmptyBorder(4, 16, 4, 0));
		ToolTips.registerTooltip(fillInCavesCheckBox, ToolTips.REMOVE_CAVES_FILL_IN);
		settingsPanel.add(fillInCavesCheckBox);
		fillInCavesCheckBox.setAlignmentX(0);

		exportIndividualBlocksCheckBox = new JCheckBox("Individual Blocks");
		exportIndividualBlocksCheckBox.setSelected(false);
		exportIndividualBlocksCheckBox.setBorder(new EmptyBorder(4, 0, 4, 0));
		ToolTips.registerTooltip(exportIndividualBlocksCheckBox, ToolTips.EXPORT_INDIVIDUAL_BLOCKS);
		settingsPanel.add(exportIndividualBlocksCheckBox);
		exportIndividualBlocksCheckBox.setAlignmentX(0);
		
		exportBlockAnimationsCheckBox = new JCheckBox("Block Animations");
		exportBlockAnimationsCheckBox.setSelected(false);
		exportBlockAnimationsCheckBox.setBorder(new EmptyBorder(4, 0, 4, 0));
		ToolTips.registerTooltip(exportBlockAnimationsCheckBox, ToolTips.EXPORT_BLOCK_ANIMATIONS);
		settingsPanel.add(exportBlockAnimationsCheckBox);
		exportBlockAnimationsCheckBox.setAlignmentX(0);
		
		excludeRegionsAsAirCheckBox = new JCheckBox("Exclude Regions as Air");
		excludeRegionsAsAirCheckBox.setSelected(false);
		excludeRegionsAsAirCheckBox.setBorder(new EmptyBorder(4, 0, 4, 0));
		ToolTips.registerTooltip(excludeRegionsAsAirCheckBox, ToolTips.EXCLUDE_REGIONS_AS_AIR);
		settingsPanel.add(excludeRegionsAsAirCheckBox);
		excludeRegionsAsAirCheckBox.setAlignmentX(0);
		
		settingsPanel.setAlignmentY(0f);
		
		settingsAndEntitiesPanel.add(settingsPanel);
		
		JPanel entityPanel = new JPanel();
		entityPanel.setLayout(new BoxLayout(entityPanel, BoxLayout.Y_AXIS));
		entityPanel.setMinimumSize(new Dimension(144, 100));
		entityPanel.setMaximumSize(entityPanel.getMinimumSize());
		entityPanel.setPreferredSize(entityPanel.getMinimumSize());
		entityPanel.add(new JPanel());
		entityButton = new JButton("Entities");
		entityButton.setPreferredSize(new Dimension(84, 84));
		entityButton.setMinimumSize(entityButton.getPreferredSize());
		entityButton.setMaximumSize(entityButton.getPreferredSize());
		entityButton.setFocusable(false);
		entityButton.setAlignmentX(0.5f);
		entityButton.setAlignmentY(0f);
		ToolTips.registerTooltip(entityButton, ToolTips.ENTITY_DIALOG);
		entityPanel.add(entityButton);
		entityPanel.add(new JPanel());
		entityPanel.setAlignmentY(0f);
		settingsAndEntitiesPanel.add(entityPanel);
		
		exportRegionActAsExcludeRegion = new JCheckBox("Export Region act as Exclude Region");
		exportRegionActAsExcludeRegion.setSelected(false);
		exportRegionActAsExcludeRegion.setBorder(new EmptyBorder(4, 0, 4, 0));
		ToolTips.registerTooltip(exportRegionActAsExcludeRegion, ToolTips.EXPORT_REGION_ACT_AS_EXCLUDE_REGION);
		leftPanel.add(exportRegionActAsExcludeRegion);
		exportRegionActAsExcludeRegion.setAlignmentX(0);
		
		leftPanel.add(new JPanel());
		
		
		lodCenterXSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setLodCenterX((Integer) lodCenterXSpinner.getValue());
				if(MCWorldExporter.getApp().getActiveExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getActiveExportBounds().hasLod());
			}

		});
		
		lodCenterZSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setLodCenterZ((Integer) lodCenterZSpinner.getValue());
				if(MCWorldExporter.getApp().getActiveExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getActiveExportBounds().hasLod());
			}

		});
		
		lodWidthSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setLodWidth((Integer) lodWidthSpinner.getValue());
				if(MCWorldExporter.getApp().getActiveExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getActiveExportBounds().hasLod());
			}

		});
		
		lodDepthSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setLodDepth((Integer) lodDepthSpinner.getValue());
				if(MCWorldExporter.getApp().getActiveExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getActiveExportBounds().hasLod());
			}

		});
		
		lodYDetailSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setLodYDetail((Integer) lodYDetailSpinner.getValue());
			}

		});
		
		lodBaseLevelSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setLodBaseLevel((Integer) lodBaseLevelSpinner.getValue());
			}

		});
		
		lodEnableCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if(MCWorldExporter.getApp().getActiveExportBounds().hasLod() == lodEnableCheckBox.isSelected())
					return;
				if(!lodEnableCheckBox.isSelected()) {
					MCWorldExporter.getApp().getActiveExportBounds().disableLod();
				}else {
					MCWorldExporter.getApp().getActiveExportBounds().enableLod();
					int lodCenterX = MCWorldExporter.getApp().getActiveExportBounds().getLodCenterX();
					int lodCenterZ = MCWorldExporter.getApp().getActiveExportBounds().getLodCenterZ();
					if(lodCenterX < MCWorldExporter.getApp().getActiveExportBounds().getMinX() || 
							lodCenterX > MCWorldExporter.getApp().getActiveExportBounds().getMaxX() ||
							lodCenterZ < MCWorldExporter.getApp().getActiveExportBounds().getMinZ() ||
							lodCenterZ > MCWorldExporter.getApp().getActiveExportBounds().getMaxZ() ||
							MCWorldExporter.getApp().getActiveExportBounds().getLodWidth() <= 1 || 
							MCWorldExporter.getApp().getActiveExportBounds().getLodDepth() <= 1) {
						// The LOD area is outside of our selection, so reset the LOD area.
						MCWorldExporter.getApp().getActiveExportBounds().setLodCenterX(MCWorldExporter.getApp().getActiveExportBounds().getCenterX());
						MCWorldExporter.getApp().getActiveExportBounds().setLodCenterZ(MCWorldExporter.getApp().getActiveExportBounds().getCenterZ());
						MCWorldExporter.getApp().getActiveExportBounds().setLodWidth(Math.min(1024, 
														MCWorldExporter.getApp().getActiveExportBounds().getWidth()/2));
						MCWorldExporter.getApp().getActiveExportBounds().setLodDepth(Math.min(1024,
														MCWorldExporter.getApp().getActiveExportBounds().getDepth()/2));
					}
				}
			}
			
		});
		
		
		runOptimiserCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Config.runOptimiser = runOptimiserCheckBox.isSelected();
			}

		});
		
		removeCavesCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Config.removeCaves = removeCavesCheckBox.isSelected();
			}

		});
		
		fillInCavesCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Config.fillInCaves = fillInCavesCheckBox.isSelected();
			}

		});
		
		exportIndividualBlocksCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setOnlyIndividualBlocks(exportIndividualBlocksCheckBox.isSelected());
			}

		});
		
		exportBlockAnimationsCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Config.exportBlockAnimations = exportBlockAnimationsCheckBox.isSelected();
			}

		});
		
		excludeRegionsAsAirCheckBox.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setExcludeRegionsAsAir(excludeRegionsAsAirCheckBox.isSelected());
			}
		});
		
		exportRegionActAsExcludeRegion.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setActAsExcludeRegion(exportRegionActAsExcludeRegion.isSelected());
			}
		});
		
		entityButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MCWorldExporter.getApp().getUI().getEntityDialog().setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				MCWorldExporter.getApp().getUI().getEntityDialog().setVisible(true);
			}
			
		});
	}
	
	public void update() {
		this.exportRegionsManager.update();
		this.excludeRegionsManager.update();
		
		if (MCWorldExporter.getApp().getActiveExportBounds().getLodCenterX() != ((Integer) lodCenterXSpinner.getValue()).intValue()) {
			lodCenterXSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getLodCenterX());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getLodCenterZ() != ((Integer) lodCenterZSpinner.getValue()).intValue()) {
			lodCenterZSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getLodCenterZ());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getLodWidth() != ((Integer) lodWidthSpinner.getValue()).intValue()) {
			lodWidthSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getLodWidth());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getLodDepth() != ((Integer) lodDepthSpinner.getValue()).intValue()) {
			lodDepthSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getLodDepth());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getLodYDetail() != ((Integer) lodYDetailSpinner.getValue()).intValue()) {
			lodYDetailSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getLodYDetail());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getLodBaseLevel() != ((Integer) lodBaseLevelSpinner.getValue()).intValue()) {
			lodBaseLevelSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getLodBaseLevel());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().hasLod() != lodEnableCheckBox.isSelected()) {
			lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getActiveExportBounds().hasLod());
		}
		if (Config.runOptimiser != runOptimiserCheckBox.isSelected()) {
			runOptimiserCheckBox.setSelected(Config.runOptimiser);
		}
		if (Config.removeCaves != removeCavesCheckBox.isSelected()) {
			removeCavesCheckBox.setSelected(Config.removeCaves);
		}
		if (Config.fillInCaves != fillInCavesCheckBox.isSelected()) {
			fillInCavesCheckBox.setSelected(Config.fillInCaves);
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().isOnlyIndividualBlocks() != exportIndividualBlocksCheckBox.isSelected()) {
			exportIndividualBlocksCheckBox.setSelected(MCWorldExporter.getApp().getActiveExportBounds().isOnlyIndividualBlocks());
		}
		if (Config.exportBlockAnimations != exportBlockAnimationsCheckBox.isSelected()) {
			exportBlockAnimationsCheckBox.setSelected(Config.exportBlockAnimations);
		}
		if(MCWorldExporter.getApp().getActiveExportBounds().isExcludeRegionsAsAir() != excludeRegionsAsAirCheckBox.isSelected()) {
			excludeRegionsAsAirCheckBox.setSelected(MCWorldExporter.getApp().getActiveExportBounds().isExcludeRegionsAsAir());
		}
		if(MCWorldExporter.getApp().getActiveExportBounds().isActAsExcludeRegion() != exportRegionActAsExcludeRegion.isSelected()) {
			exportRegionActAsExcludeRegion.setSelected(MCWorldExporter.getApp().getActiveExportBounds().isActAsExcludeRegion());
		}
	}

}
