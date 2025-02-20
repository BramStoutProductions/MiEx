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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.export.Converter;
import nl.bramstout.mcworldexporter.export.ExportData;
import nl.bramstout.mcworldexporter.export.Exporter;
import nl.bramstout.mcworldexporter.parallel.BackgroundThread;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.World;

public class ToolBar extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JButton loadWorldButton;
	private JButton loadWorldFromExportButton;
	private JToggleButton pauseLoadingButton;
	private JComboBox<String> dimensionChooser;

	private JSpinner minXSpinner;
	private JSpinner minYSpinner;
	private JSpinner minZSpinner;
	private JSpinner maxXSpinner;
	private JSpinner maxYSpinner;
	private JSpinner maxZSpinner;
	private JSpinner yOffsetSpinner;
	private JCheckBox yOffsetAutoCheckBox;
	private Object prevYOffsetMutex = new Object();
	private int prevYOffset;
	private World prevWorld;
	
	private JCheckBox lodEnableCheckBox;
	private JSpinner lodCenterXSpinner;
	private JSpinner lodCenterZSpinner;
	private JSpinner lodWidthSpinner;
	private JSpinner lodDepthSpinner;
	private JSpinner lodYDetailSpinner;

	private JButton teleportButton;
	private JButton zoomOutButton;
	private JButton zoomInButton;

	private JCheckBox runOptimiserCheckBox;
	private JCheckBox removeCavesCheckBox;
	private JCheckBox fillInCavesCheckBox;
	private JCheckBox exportIndividualBlocksCheckBox;
	private JSpinner chunkSizeSpinner;

	private JToggleButton editFGChunksButton;
	
	private JButton entityButton;

	private JButton exportButton;
	
	private File exportLastDirectory;
	
	private WorldBrowser worldBrowser;
	
	private AboutDialog aboutDialog;

	public ToolBar() {
		super();
		
		prevWorld = null;
		
		aboutDialog = new AboutDialog();
		
		enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
		
		exportLastDirectory = null;

		setPreferredSize(new Dimension(1200, 156));
		setMinimumSize(new Dimension(128, 156));
		setMaximumSize(new Dimension(100000, 156));

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		setBorder(new EmptyBorder(8, 8, 8, 8));

		add(new JPanel());
		add(new JPanel());
		add(new JPanel());

		JPanel worldPanel = new JPanel();
		worldPanel.setLayout(new BoxLayout(worldPanel, BoxLayout.Y_AXIS));
		worldPanel.setMinimumSize(new Dimension(300, 140));
		worldPanel.setMaximumSize(worldPanel.getMinimumSize());
		worldPanel.setPreferredSize(worldPanel.getMinimumSize());
		JLabel worldPanelLabel = new JLabel("World");
		worldPanelLabel.setAlignmentX(0.5f);
		worldPanel.add(worldPanelLabel);
		worldPanel.add(new JPanel());
		JPanel worldCtrlPanel = new JPanel();
		worldCtrlPanel.setLayout(new BoxLayout(worldCtrlPanel, BoxLayout.X_AXIS));
		
		JPanel loadButtonsPanel = new JPanel();
		loadButtonsPanel.setLayout(new BoxLayout(loadButtonsPanel, BoxLayout.Y_AXIS));
		loadButtonsPanel.setBorder(new EmptyBorder(0,0,0,0));
		worldCtrlPanel.add(loadButtonsPanel);
		
		loadWorldButton = new JButton("Load");
		loadWorldButton.setPreferredSize(new Dimension(130, 24));
		loadWorldButton.setMinimumSize(loadWorldButton.getPreferredSize());
		loadWorldButton.setMaximumSize(loadWorldButton.getPreferredSize());
		loadWorldButton.setToolTipText("Load in a Minecraft world.");
		loadButtonsPanel.add(loadWorldButton);
		
		JPanel loadButtonsPadding = new JPanel();
		loadButtonsPadding.setPreferredSize(new Dimension(10, 4));
		loadButtonsPadding.setMinimumSize(new Dimension(10, 4));
		loadButtonsPadding.setMaximumSize(new Dimension(10, 4));
		loadButtonsPadding.setBorder(new EmptyBorder(0,0,0,0));
		loadButtonsPanel.add(loadButtonsPadding);
		
		loadWorldFromExportButton = new JButton("Load From Export");
		loadWorldFromExportButton.setPreferredSize(new Dimension(130, 24));
		loadWorldFromExportButton.setMinimumSize(loadWorldFromExportButton.getPreferredSize());
		loadWorldFromExportButton.setMaximumSize(loadWorldFromExportButton.getPreferredSize());
		ToolTips.registerTooltip(loadWorldFromExportButton, ToolTips.LOAD_WORLD_FROM_EXPORT);
		loadButtonsPanel.add(loadWorldFromExportButton);
		
		JPanel loadButtonsPadding2 = new JPanel();
		loadButtonsPadding2.setPreferredSize(new Dimension(10, 8));
		loadButtonsPadding2.setMinimumSize(new Dimension(10, 8));
		loadButtonsPadding2.setMaximumSize(new Dimension(10, 8));
		loadButtonsPadding2.setBorder(new EmptyBorder(0,0,0,0));
		loadButtonsPanel.add(loadButtonsPadding2);
		
		pauseLoadingButton = new JToggleButton("Pause Loading");
		pauseLoadingButton.setPreferredSize(new Dimension(130, 24));
		pauseLoadingButton.setMinimumSize(pauseLoadingButton.getPreferredSize());
		pauseLoadingButton.setMaximumSize(pauseLoadingButton.getPreferredSize());
		ToolTips.registerTooltip(pauseLoadingButton, ToolTips.PAUSE_LOADING);
		loadButtonsPanel.add(pauseLoadingButton);
		
		worldCtrlPanel.add(new JPanel());
		dimensionChooser = new JComboBox<String>();
		dimensionChooser.setPreferredSize(new Dimension(150, 24));
		dimensionChooser.setMinimumSize(dimensionChooser.getPreferredSize());
		dimensionChooser.setMaximumSize(dimensionChooser.getPreferredSize());
		ToolTips.registerTooltip(dimensionChooser, ToolTips.DIMENSION_CHOOSER);
		worldCtrlPanel.add(dimensionChooser);
		worldPanel.add(worldCtrlPanel);
		worldPanel.add(new JPanel());
		add(worldPanel);

		add(new JPanel());

		JPanel selectionPanel = new JPanel();
		selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
		selectionPanel.setMinimumSize(new Dimension(228, 140));
		selectionPanel.setMaximumSize(selectionPanel.getMinimumSize());
		selectionPanel.setPreferredSize(selectionPanel.getMinimumSize());
		ToolTips.registerTooltip(selectionPanel, ToolTips.EXPORT_BOUNDS);
		JLabel selectionPanelLabel = new JLabel("Selection");
		selectionPanelLabel.setAlignmentX(0.5f);
		selectionPanel.add(selectionPanelLabel);
		selectionPanel.add(new JPanel());

		JPanel selectionCtrlPanel = new JPanel();
		selectionCtrlPanel.setLayout(new BoxLayout(selectionCtrlPanel, BoxLayout.X_AXIS));

		JPanel selectionLabelPanel = new JPanel();
		selectionLabelPanel.setLayout(new BoxLayout(selectionLabelPanel, BoxLayout.Y_AXIS));
		selectionLabelPanel.setPreferredSize(new Dimension(24, 80));
		selectionLabelPanel.setMinimumSize(selectionLabelPanel.getPreferredSize());
		selectionLabelPanel.setMaximumSize(selectionLabelPanel.getPreferredSize());
		selectionLabelPanel.add(new JPanel());
		JLabel selectionXLabel = new JLabel("X:");
		selectionXLabel.setPreferredSize(new Dimension(24, 20));
		selectionXLabel.setMinimumSize(selectionXLabel.getPreferredSize());
		selectionXLabel.setMaximumSize(selectionXLabel.getPreferredSize());
		selectionLabelPanel.add(selectionXLabel);
		JLabel selectionYLabel = new JLabel("Y:");
		selectionYLabel.setPreferredSize(new Dimension(24, 20));
		selectionYLabel.setMinimumSize(selectionYLabel.getPreferredSize());
		selectionYLabel.setMaximumSize(selectionYLabel.getPreferredSize());
		selectionLabelPanel.add(selectionYLabel);
		JLabel selectionZLabel = new JLabel("Z:");
		selectionZLabel.setPreferredSize(new Dimension(24, 20));
		selectionZLabel.setMinimumSize(selectionZLabel.getPreferredSize());
		selectionZLabel.setMaximumSize(selectionZLabel.getPreferredSize());
		selectionLabelPanel.add(selectionZLabel);
		selectionCtrlPanel.add(selectionLabelPanel);

		JPanel selectionMinPanel = new JPanel();
		selectionMinPanel.setLayout(new BoxLayout(selectionMinPanel, BoxLayout.Y_AXIS));
		selectionMinPanel.setPreferredSize(new Dimension(96, 80));
		selectionMinPanel.setMinimumSize(selectionMinPanel.getPreferredSize());
		selectionMinPanel.setMaximumSize(selectionMinPanel.getPreferredSize());
		JLabel minLabel = new JLabel("Min");
		minLabel.setBorder(null);
		minLabel.setAlignmentX(0.5f);
		minLabel.setHorizontalAlignment(SwingConstants.CENTER);
		minLabel.setPreferredSize(new Dimension(96, 20));
		minLabel.setMinimumSize(minLabel.getPreferredSize());
		minLabel.setMaximumSize(minLabel.getPreferredSize());
		selectionMinPanel.add(minLabel);
		minXSpinner = new JSpinner();
		minXSpinner.setPreferredSize(new Dimension(96, 20));
		minXSpinner.setMinimumSize(minXSpinner.getPreferredSize());
		minXSpinner.setMaximumSize(minXSpinner.getPreferredSize());
		selectionMinPanel.add(minXSpinner);
		minYSpinner = new JSpinner();
		minYSpinner.setPreferredSize(new Dimension(96, 20));
		minYSpinner.setMinimumSize(minYSpinner.getPreferredSize());
		minYSpinner.setMaximumSize(minYSpinner.getPreferredSize());
		minYSpinner.setValue(-64);
		selectionMinPanel.add(minYSpinner);
		minZSpinner = new JSpinner();
		minZSpinner.setPreferredSize(new Dimension(96, 20));
		minZSpinner.setMinimumSize(minZSpinner.getPreferredSize());
		minZSpinner.setMaximumSize(minZSpinner.getPreferredSize());
		selectionMinPanel.add(minZSpinner);
		selectionCtrlPanel.add(selectionMinPanel);

		selectionCtrlPanel.add(new JPanel());

		JPanel selectionMaxPanel = new JPanel();
		selectionMaxPanel.setLayout(new BoxLayout(selectionMaxPanel, BoxLayout.Y_AXIS));
		selectionMaxPanel.setPreferredSize(new Dimension(96, 80));
		selectionMaxPanel.setMinimumSize(selectionMaxPanel.getPreferredSize());
		selectionMaxPanel.setMaximumSize(selectionMaxPanel.getPreferredSize());
		JLabel maxLabel = new JLabel("Max");
		maxLabel.setBorder(null);
		maxLabel.setAlignmentX(0.5f);
		maxLabel.setHorizontalAlignment(SwingConstants.CENTER);
		maxLabel.setPreferredSize(new Dimension(96, 20));
		maxLabel.setMinimumSize(maxLabel.getPreferredSize());
		maxLabel.setMaximumSize(maxLabel.getPreferredSize());
		selectionMaxPanel.add(maxLabel);
		maxXSpinner = new JSpinner();
		maxXSpinner.setPreferredSize(new Dimension(96, 20));
		maxXSpinner.setMinimumSize(maxXSpinner.getPreferredSize());
		maxXSpinner.setMaximumSize(maxXSpinner.getPreferredSize());
		selectionMaxPanel.add(maxXSpinner);
		maxYSpinner = new JSpinner();
		maxYSpinner.setPreferredSize(new Dimension(96, 20));
		maxYSpinner.setMinimumSize(maxYSpinner.getPreferredSize());
		maxYSpinner.setMaximumSize(maxYSpinner.getPreferredSize());
		maxYSpinner.setValue(320);
		selectionMaxPanel.add(maxYSpinner);
		maxZSpinner = new JSpinner();
		maxZSpinner.setPreferredSize(new Dimension(96, 20));
		maxZSpinner.setMinimumSize(maxZSpinner.getPreferredSize());
		maxZSpinner.setMaximumSize(maxZSpinner.getPreferredSize());
		selectionMaxPanel.add(maxZSpinner);
		selectionCtrlPanel.add(selectionMaxPanel);

		selectionPanel.add(selectionCtrlPanel);
		
		JPanel selectionOffsetPanel = new JPanel();
		selectionOffsetPanel.setLayout(new BoxLayout(selectionOffsetPanel, BoxLayout.X_AXIS));
		selectionOffsetPanel.setBorder(new EmptyBorder(9, 0, 0, 0));
		ToolTips.registerTooltip(selectionOffsetPanel, ToolTips.Y_OFFSET);
		JLabel yOffsetLabel = new JLabel("Y Origin:  ");
		selectionOffsetPanel.add(yOffsetLabel);
		yOffsetSpinner = new JSpinner();
		yOffsetSpinner.setPreferredSize(new Dimension(96, 20));
		yOffsetSpinner.setMinimumSize(yOffsetSpinner.getPreferredSize());
		yOffsetSpinner.setMaximumSize(yOffsetSpinner.getPreferredSize());
		prevYOffset = 64;
		yOffsetSpinner.setValue(64);
		selectionOffsetPanel.add(yOffsetSpinner);
		yOffsetAutoCheckBox = new JCheckBox("Auto");
		yOffsetAutoCheckBox.setSelected(true);
		selectionOffsetPanel.add(yOffsetAutoCheckBox);
		selectionPanel.add(selectionOffsetPanel);

		selectionPanel.add(new JPanel());
		add(selectionPanel);

		add(new JPanel());
		
		JPanel lodPanel = new JPanel();
		lodPanel.setLayout(new BoxLayout(lodPanel, BoxLayout.Y_AXIS));
		lodPanel.setMinimumSize(new Dimension(144, 140));
		lodPanel.setMaximumSize(selectionPanel.getMinimumSize());
		lodPanel.setPreferredSize(selectionPanel.getMinimumSize());
		ToolTips.registerTooltip(lodPanel, ToolTips.LOD);
		lodEnableCheckBox = new JCheckBox("LOD");
		lodEnableCheckBox.setSelected(false);
		ToolTips.registerTooltip(lodEnableCheckBox, ToolTips.LOD_ENABLE);
		lodPanel.add(lodEnableCheckBox);
		lodPanel.add(new JPanel());

		JPanel lodCtrlPanel = new JPanel();
		lodCtrlPanel.setLayout(new BoxLayout(lodCtrlPanel, BoxLayout.X_AXIS));

		JPanel lodLabelPanel = new JPanel();
		lodLabelPanel.setLayout(new BoxLayout(lodLabelPanel, BoxLayout.Y_AXIS));
		lodLabelPanel.setPreferredSize(new Dimension(48, 100));
		lodLabelPanel.setMinimumSize(lodLabelPanel.getPreferredSize());
		lodLabelPanel.setMaximumSize(lodLabelPanel.getPreferredSize());
		JLabel lodXLabel = new JLabel("X:");
		lodXLabel.setPreferredSize(new Dimension(48, 20));
		lodXLabel.setMinimumSize(lodXLabel.getPreferredSize());
		lodXLabel.setMaximumSize(lodXLabel.getPreferredSize());
		lodLabelPanel.add(lodXLabel);
		JLabel lodZLabel = new JLabel("Z:");
		lodZLabel.setPreferredSize(new Dimension(48, 20));
		lodZLabel.setMinimumSize(lodZLabel.getPreferredSize());
		lodZLabel.setMaximumSize(lodZLabel.getPreferredSize());
		lodLabelPanel.add(lodZLabel);
		JLabel lodWidthLabel = new JLabel("Width:");
		lodWidthLabel.setPreferredSize(new Dimension(48, 20));
		lodWidthLabel.setMinimumSize(lodWidthLabel.getPreferredSize());
		lodWidthLabel.setMaximumSize(lodWidthLabel.getPreferredSize());
		lodLabelPanel.add(lodWidthLabel);
		JLabel lodDepthLabel = new JLabel("Depth:");
		lodDepthLabel.setPreferredSize(new Dimension(48, 20));
		lodDepthLabel.setMinimumSize(lodDepthLabel.getPreferredSize());
		lodDepthLabel.setMaximumSize(lodDepthLabel.getPreferredSize());
		lodLabelPanel.add(lodDepthLabel);
		JLabel lodYDetailLabel = new JLabel("Y Detail:");
		lodYDetailLabel.setPreferredSize(new Dimension(48, 20));
		lodYDetailLabel.setMinimumSize(lodYDetailLabel.getPreferredSize());
		lodYDetailLabel.setMaximumSize(lodYDetailLabel.getPreferredSize());
		ToolTips.registerTooltip(lodYDetailLabel, ToolTips.LOD_Y_DETAIL);
		lodLabelPanel.add(lodYDetailLabel);
		lodCtrlPanel.add(lodLabelPanel);

		JPanel lodSpinnerPanel = new JPanel();
		lodSpinnerPanel.setLayout(new BoxLayout(lodSpinnerPanel, BoxLayout.Y_AXIS));
		lodSpinnerPanel.setPreferredSize(new Dimension(96, 100));
		lodSpinnerPanel.setMinimumSize(lodSpinnerPanel.getPreferredSize());
		lodSpinnerPanel.setMaximumSize(lodSpinnerPanel.getPreferredSize());
		lodCenterXSpinner = new JSpinner();
		lodCenterXSpinner.setPreferredSize(new Dimension(96, 20));
		lodCenterXSpinner.setMinimumSize(lodCenterXSpinner.getPreferredSize());
		lodCenterXSpinner.setMaximumSize(lodCenterXSpinner.getPreferredSize());
		lodSpinnerPanel.add(lodCenterXSpinner);
		lodCenterZSpinner = new JSpinner();
		lodCenterZSpinner.setPreferredSize(new Dimension(96, 20));
		lodCenterZSpinner.setMinimumSize(lodCenterZSpinner.getPreferredSize());
		lodCenterZSpinner.setMaximumSize(lodCenterZSpinner.getPreferredSize());
		lodSpinnerPanel.add(lodCenterZSpinner);
		lodWidthSpinner = new JSpinner();
		lodWidthSpinner.setPreferredSize(new Dimension(96, 20));
		lodWidthSpinner.setMinimumSize(lodWidthSpinner.getPreferredSize());
		lodWidthSpinner.setMaximumSize(lodWidthSpinner.getPreferredSize());
		lodSpinnerPanel.add(lodWidthSpinner);
		lodDepthSpinner = new JSpinner();
		lodDepthSpinner.setPreferredSize(new Dimension(96, 20));
		lodDepthSpinner.setMinimumSize(lodDepthSpinner.getPreferredSize());
		lodDepthSpinner.setMaximumSize(lodDepthSpinner.getPreferredSize());
		lodSpinnerPanel.add(lodDepthSpinner);
		lodYDetailSpinner = new JSpinner();
		lodYDetailSpinner.setPreferredSize(new Dimension(96, 20));
		lodYDetailSpinner.setValue(4);
		((SpinnerNumberModel)(lodYDetailSpinner.getModel())).setMinimum(1);
		((SpinnerNumberModel)(lodYDetailSpinner.getModel())).setMaximum(16);
		lodYDetailSpinner.setMinimumSize(lodYDetailSpinner.getPreferredSize());
		lodYDetailSpinner.setMaximumSize(lodYDetailSpinner.getPreferredSize());
		ToolTips.registerTooltip(lodYDetailSpinner, ToolTips.LOD_Y_DETAIL);
		lodSpinnerPanel.add(lodYDetailSpinner);
		lodCtrlPanel.add(lodSpinnerPanel);

		lodPanel.add(lodCtrlPanel);

		lodPanel.add(new JPanel());
		add(lodPanel);

		add(new JPanel());

		JPanel zoomPanel = new JPanel();
		zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.Y_AXIS));
		zoomPanel.setMinimumSize(new Dimension(150, 140));
		zoomPanel.setMaximumSize(zoomPanel.getMinimumSize());
		zoomPanel.setPreferredSize(zoomPanel.getMinimumSize());
		JLabel zoomPanelLabel = new JLabel("Zoom");
		zoomPanelLabel.setAlignmentX(0.5f);
		zoomPanel.add(zoomPanelLabel);
		zoomPanel.add(new JPanel());
		JPanel zoomCtrlPanel = new JPanel();
		zoomCtrlPanel.setLayout(new BoxLayout(zoomCtrlPanel, BoxLayout.X_AXIS));
		zoomOutButton = new JButton("-");
		zoomOutButton.setPreferredSize(new Dimension(48, 48));
		zoomOutButton.setMinimumSize(zoomOutButton.getPreferredSize());
		zoomOutButton.setMaximumSize(zoomOutButton.getPreferredSize());
		ToolTips.registerTooltip(zoomOutButton, ToolTips.ZOOM_OUT);
		zoomCtrlPanel.add(zoomOutButton);
		zoomInButton = new JButton("+");
		zoomInButton.setPreferredSize(new Dimension(48, 48));
		zoomInButton.setMinimumSize(zoomInButton.getPreferredSize());
		zoomInButton.setMaximumSize(zoomInButton.getPreferredSize());
		ToolTips.registerTooltip(zoomInButton, ToolTips.ZOOM_IN);
		zoomCtrlPanel.add(zoomInButton);
		teleportButton = new JButton("TP");
		teleportButton.setPreferredSize(new Dimension(48, 48));
		teleportButton.setMinimumSize(teleportButton.getPreferredSize());
		teleportButton.setMaximumSize(teleportButton.getPreferredSize());
		ToolTips.registerTooltip(teleportButton, ToolTips.TELEPORT);
		zoomCtrlPanel.add(teleportButton);
		zoomPanel.add(zoomCtrlPanel);
		zoomPanel.add(new JPanel());
		add(zoomPanel);

		add(new JPanel());

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		settingsPanel.add(new JLabel(" "));
		settingsPanel.add(new JPanel());
		runOptimiserCheckBox = new JCheckBox("Run Optimisers");
		runOptimiserCheckBox.setSelected(Config.runOptimiser);
		ToolTips.registerTooltip(runOptimiserCheckBox, ToolTips.RUN_OPTIMISERS);
		settingsPanel.add(runOptimiserCheckBox);
		runOptimiserCheckBox.setAlignmentX(0);
		
		removeCavesCheckBox = new JCheckBox("Remove Caves");
		removeCavesCheckBox.setSelected(Config.removeCaves);
		ToolTips.registerTooltip(removeCavesCheckBox, ToolTips.REMOVE_CAVES);
		settingsPanel.add(removeCavesCheckBox);
		removeCavesCheckBox.setAlignmentX(0);
		
		fillInCavesCheckBox = new JCheckBox("Fill In Caves");
		fillInCavesCheckBox.setSelected(Config.fillInCaves);
		fillInCavesCheckBox.setBorder(new EmptyBorder(0, 16, 0, 0));
		ToolTips.registerTooltip(fillInCavesCheckBox, ToolTips.REMOVE_CAVES_FILL_IN);
		settingsPanel.add(fillInCavesCheckBox);
		fillInCavesCheckBox.setAlignmentX(0);

		exportIndividualBlocksCheckBox = new JCheckBox("Individual Blocks");
		exportIndividualBlocksCheckBox.setSelected(Config.onlyIndividualBlocks);
		ToolTips.registerTooltip(exportIndividualBlocksCheckBox, ToolTips.EXPORT_INDIVIDUAL_BLOCKS);
		settingsPanel.add(exportIndividualBlocksCheckBox);
		exportIndividualBlocksCheckBox.setAlignmentX(0);
		
		JPanel chunkSizePanel = new JPanel();
		chunkSizePanel.setLayout(new BoxLayout(chunkSizePanel, BoxLayout.X_AXIS));
		ToolTips.registerTooltip(chunkSizePanel, ToolTips.CHUNK_SIZE);
		JLabel chunkSizeLabel = new JLabel("Chunk Size:");
		chunkSizeLabel.setBorder(new EmptyBorder(0, 6, 0, 8));
		chunkSizePanel.add(chunkSizeLabel);
		chunkSizeSpinner = new JSpinner();
		chunkSizeSpinner.setValue(Config.chunkSize);
		((SpinnerNumberModel)(chunkSizeSpinner.getModel())).setMinimum(1);
		((SpinnerNumberModel)(chunkSizeSpinner.getModel())).setMaximum(1024);
		chunkSizeSpinner.setPreferredSize(new Dimension(48, 20));
		chunkSizeSpinner.setMinimumSize(new Dimension(48, 20));
		chunkSizeSpinner.setMaximumSize(new Dimension(48, 20));
		chunkSizePanel.add(chunkSizeSpinner);
		settingsPanel.add(chunkSizePanel);
		chunkSizePanel.setAlignmentX(0);

		settingsPanel.add(new JPanel());
		add(settingsPanel);

		add(new JPanel());

		JPanel editFGChunksPanel = new JPanel();
		editFGChunksPanel.setLayout(new BoxLayout(editFGChunksPanel, BoxLayout.Y_AXIS));
		editFGChunksPanel.setMinimumSize(new Dimension(90, 140));
		editFGChunksPanel.setMaximumSize(editFGChunksPanel.getMinimumSize());
		editFGChunksPanel.setPreferredSize(editFGChunksPanel.getMinimumSize());
		editFGChunksPanel.add(new JLabel(" "));
		editFGChunksPanel.add(new JPanel());
		editFGChunksButton = new JToggleButton("Edit FG");
		editFGChunksButton.setPreferredSize(new Dimension(84, 84));
		editFGChunksButton.setMinimumSize(editFGChunksButton.getPreferredSize());
		editFGChunksButton.setMaximumSize(editFGChunksButton.getPreferredSize());
		editFGChunksButton.setFocusable(false);
		ToolTips.registerTooltip(editFGChunksButton, ToolTips.EDIT_FG);
		editFGChunksPanel.add(editFGChunksButton);
		editFGChunksPanel.add(new JPanel());
		add(editFGChunksPanel);
		
		add(new JPanel());

		JPanel entityPanel = new JPanel();
		entityPanel.setLayout(new BoxLayout(entityPanel, BoxLayout.Y_AXIS));
		entityPanel.setMinimumSize(new Dimension(90, 140));
		entityPanel.setMaximumSize(entityPanel.getMinimumSize());
		entityPanel.setPreferredSize(entityPanel.getMinimumSize());
		entityPanel.add(new JLabel(" "));
		entityPanel.add(new JPanel());
		entityButton = new JButton("Entities");
		entityButton.setPreferredSize(new Dimension(84, 84));
		entityButton.setMinimumSize(entityButton.getPreferredSize());
		entityButton.setMaximumSize(entityButton.getPreferredSize());
		entityButton.setFocusable(false);
		ToolTips.registerTooltip(entityButton, ToolTips.ENTITY_DIALOG);
		entityPanel.add(entityButton);
		entityPanel.add(new JPanel());
		add(entityPanel);

		add(new JPanel());

		JPanel exportPanel = new JPanel();
		exportPanel.setLayout(new BoxLayout(exportPanel, BoxLayout.Y_AXIS));
		exportPanel.setMinimumSize(new Dimension(90, 140));
		exportPanel.setMaximumSize(exportPanel.getMinimumSize());
		exportPanel.setPreferredSize(exportPanel.getMinimumSize());
		exportPanel.add(new JLabel(" "));
		exportPanel.add(new JPanel());
		exportButton = new JButton("Export");
		exportButton.setPreferredSize(new Dimension(84, 84));
		exportButton.setMinimumSize(exportButton.getPreferredSize());
		exportButton.setMaximumSize(exportButton.getPreferredSize());
		ToolTips.registerTooltip(exportButton, ToolTips.EXPORT);
		exportPanel.add(exportButton);
		exportPanel.add(new JPanel());
		add(exportPanel);

		add(new JPanel());
		add(new JPanel());
		add(new JPanel());
		
		worldBrowser = new WorldBrowser();
		
		loadWorldButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				worldBrowser.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				worldBrowser.setVisible(true);
			}

		});
		
		loadWorldFromExportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setApproveButtonText("Load");
				chooser.setDialogTitle("Load Export");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if(exportLastDirectory == null || !exportLastDirectory.exists())
					exportLastDirectory = new File(FileUtil.getHomeDir());
				chooser.setCurrentDirectory(exportLastDirectory);
				FileFilter defaultFilter = null;
				for(String extension : Converter.getExtensions()) {
					FileFilter filter = new FileNameExtensionFilter(extension.toUpperCase() + " Files", extension);
					chooser.addChoosableFileFilter(filter);
					if(defaultFilter == null)
						defaultFilter = filter;
				}
				chooser.setFileFilter(defaultFilter);
				chooser.setAcceptAllFileFilterUsed(false);
				int result = chooser.showOpenDialog(MCWorldExporter.getApp().getUI());
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					FileNameExtensionFilter filter = (FileNameExtensionFilter) chooser.getFileFilter();
					if (!file.getAbsolutePath().toLowerCase().endsWith("." + filter.getExtensions()[0]))
						file = new File(file.getAbsolutePath() + "." + filter.getExtensions()[0]);
					
					exportLastDirectory = file.getParentFile();
					
					String[] tokens = file.getName().split("\\.");
					
					Converter converter = Converter.getConverter(tokens[tokens.length-1], null, null);
					
					ExportData exportData = converter.getExportData(file);
					if(exportData != null)
						exportData.apply();
				}
			}

		});
		
		Border pauseLoadingButtonBorder = pauseLoadingButton.getBorder();
		pauseLoadingButton.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if(pauseLoadingButton.isSelected()) {
					if(MCWorldExporter.getApp().getWorld() != null)
						MCWorldExporter.getApp().getWorld().pauseLoading();
					pauseLoadingButton.setBorder(new LineBorder(new Color(0f,0.7f,1f), 4));
				}else {
					if(MCWorldExporter.getApp().getWorld() != null)
						MCWorldExporter.getApp().getWorld().unpauseLoading();
					pauseLoadingButton.setBorder(pauseLoadingButtonBorder);
				}
				MCWorldExporter.getApp().getUI().update();
			}

		});

		dimensionChooser.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					if (MCWorldExporter.getApp().getWorld() != null)
						MCWorldExporter.getApp().getWorld().loadDimension((String) e.getItem());
			}

		});

		minXSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setMinX((Integer) minXSpinner.getValue());
			}

		});

		minYSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setMinY((Integer) minYSpinner.getValue());
				MCWorldExporter.getApp().getUI().fullReRender();
			}

		});

		minZSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setMinZ((Integer) minZSpinner.getValue());
			}

		});

		maxXSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setMaxX((Integer) maxXSpinner.getValue());
			}

		});

		maxYSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setMaxY((Integer) maxYSpinner.getValue());
				MCWorldExporter.getApp().getUI().fullReRender();
			}

		});

		maxZSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setMaxZ((Integer) maxZSpinner.getValue());
			}

		});
		
		yOffsetSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setOffsetY((Integer) yOffsetSpinner.getValue());
				synchronized(prevYOffsetMutex) {
					if(yOffsetAutoCheckBox.isSelected() && ((Integer) yOffsetSpinner.getValue()) != prevYOffset) {
						// If we're in auto mode, but the value in the spinner doesn't match
						// the value in the prevYOffset, then the user manually changed it.
						// So, we need to turn auto off.
						yOffsetAutoCheckBox.setSelected(false);
					}
				}
			}
			
		});
		
		yOffsetAutoCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				update();
			}
			
		});
		
		lodCenterXSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setLodCenterX((Integer) lodCenterXSpinner.getValue());
				if(MCWorldExporter.getApp().getExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getExportBounds().hasLod());
			}

		});
		
		lodCenterZSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setLodCenterZ((Integer) lodCenterZSpinner.getValue());
				if(MCWorldExporter.getApp().getExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getExportBounds().hasLod());
			}

		});
		
		lodWidthSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setLodWidth((Integer) lodWidthSpinner.getValue());
				if(MCWorldExporter.getApp().getExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getExportBounds().hasLod());
			}

		});
		
		lodDepthSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setLodDepth((Integer) lodDepthSpinner.getValue());
				if(MCWorldExporter.getApp().getExportBounds().hasLod() != lodEnableCheckBox.isSelected())
					lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getExportBounds().hasLod());
			}

		});
		
		lodYDetailSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBounds().setLodYDetail((Integer) lodYDetailSpinner.getValue());
			}

		});
		
		lodEnableCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if(MCWorldExporter.getApp().getExportBounds().hasLod() == lodEnableCheckBox.isSelected())
					return;
				if(!lodEnableCheckBox.isSelected()) {
					MCWorldExporter.getApp().getExportBounds().disableLod();
				}else {
					MCWorldExporter.getApp().getExportBounds().enableLod();
					int lodCenterX = MCWorldExporter.getApp().getExportBounds().getLodCenterX();
					int lodCenterZ = MCWorldExporter.getApp().getExportBounds().getLodCenterZ();
					if(lodCenterX < MCWorldExporter.getApp().getExportBounds().getMinX() || 
							lodCenterX > MCWorldExporter.getApp().getExportBounds().getMaxX() ||
							lodCenterZ < MCWorldExporter.getApp().getExportBounds().getMinZ() ||
							lodCenterZ > MCWorldExporter.getApp().getExportBounds().getMaxZ() ||
							MCWorldExporter.getApp().getExportBounds().getLodWidth() <= 1 || 
							MCWorldExporter.getApp().getExportBounds().getLodDepth() <= 1) {
						// The LOD area is outside of our selection, so reset the LOD area.
						MCWorldExporter.getApp().getExportBounds().setLodCenterX(MCWorldExporter.getApp().getExportBounds().getCenterX());
						MCWorldExporter.getApp().getExportBounds().setLodCenterZ(MCWorldExporter.getApp().getExportBounds().getCenterZ());
						MCWorldExporter.getApp().getExportBounds().setLodWidth(Math.min(1024, 
														MCWorldExporter.getApp().getExportBounds().getWidth()/2));
						MCWorldExporter.getApp().getExportBounds().setLodDepth(Math.min(1024,
														MCWorldExporter.getApp().getExportBounds().getDepth()/2));
					}
				}
			}
			
		});

		zoomOutButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MCWorldExporter.getApp().getUI().getViewer().zoomOut();
			}

		});

		zoomInButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MCWorldExporter.getApp().getUI().getViewer().zoomIn();
			}

		});

		teleportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				TeleportDialog dialog = new TeleportDialog();
				dialog.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				dialog.setVisible(true);
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
				Config.onlyIndividualBlocks = exportIndividualBlocksCheckBox.isSelected();
			}

		});
		
		chunkSizeSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Config.chunkSize = ((Integer) chunkSizeSpinner.getValue()).intValue();
			}

		});

		Border editFGChunksButtonBorder = editFGChunksButton.getBorder();
		editFGChunksButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MCWorldExporter.getApp().getUI().update();
				if(editFGChunksButton.isSelected()) {
					editFGChunksButton.setBorder(new LineBorder(new Color(0f,0.7f,1f), 4));
				}else {
					editFGChunksButton.setBorder(editFGChunksButtonBorder);
				}
			}

		});

		entityButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MCWorldExporter.getApp().getUI().getEntityDialog().setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				MCWorldExporter.getApp().getUI().getEntityDialog().setVisible(true);
			}
			
		});
		
		exportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Config.runOptimiser = runOptimiserCheckBox.isSelected();
				Config.removeCaves = removeCavesCheckBox.isSelected();
				Config.fillInCaves = Config.removeCaves && fillInCavesCheckBox.isSelected();
				Config.onlyIndividualBlocks = exportIndividualBlocksCheckBox.isSelected();
				JFileChooser chooser = new JFileChooser();
				chooser.setApproveButtonText("Export");
				chooser.setDialogTitle("Export World");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if(exportLastDirectory == null || !exportLastDirectory.exists())
					exportLastDirectory = new File(FileUtil.getHomeDir());
				chooser.setCurrentDirectory(exportLastDirectory);
				FileFilter defaultFilter = null;
				for(String extension : Converter.getExtensions()) {
					FileFilter filter = new FileNameExtensionFilter(extension.toUpperCase() + " Files", extension);
					chooser.addChoosableFileFilter(filter);
					if(defaultFilter == null)
						defaultFilter = filter;
				}
				chooser.setFileFilter(defaultFilter);
				chooser.setAcceptAllFileFilterUsed(false);
				int result = chooser.showSaveDialog(MCWorldExporter.getApp().getUI());
				if (result == JFileChooser.APPROVE_OPTION) {
					try {
						File file = chooser.getSelectedFile();
						FileNameExtensionFilter filter = (FileNameExtensionFilter) chooser.getFileFilter();
						if (!file.getAbsolutePath().toLowerCase().endsWith("." + filter.getExtensions()[0]))
							file = new File(file.getAbsolutePath() + "." + filter.getExtensions()[0]);
						
						exportLastDirectory = file.getParentFile();
						
						Exporter.export(file);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}

		});
	}
	
	private int prevCenterX;
	private int prevCenterZ;

	public void update() {
		if (MCWorldExporter.getApp().getWorld() == null) {
			dimensionChooser.removeAllItems();
		} else {
			if(pauseLoadingButton.isSelected() != MCWorldExporter.getApp().getWorld().isPaused())
				pauseLoadingButton.setSelected(MCWorldExporter.getApp().getWorld().isPaused());
			
			boolean needUpdate = false;
			for (String dimension : MCWorldExporter.getApp().getWorld().getDimensions()) {
				boolean found = false;
				for (int i = 0; i < dimensionChooser.getModel().getSize(); ++i) {
					if (dimensionChooser.getModel().getElementAt(i).equals(dimension)) {
						found = true;
						break;
					}
				}
				if (!found) {
					needUpdate = true;
					break;
				}
			}
			if (needUpdate) {
				dimensionChooser.removeAllItems();
				for (String dimension : MCWorldExporter.getApp().getWorld().getDimensions()) {
					dimensionChooser.addItem(dimension);
				}
				dimensionChooser.setSelectedItem(MCWorldExporter.getApp().getWorld().getCurrentDimensions());
			}
			if(!dimensionChooser.getSelectedItem().equals(MCWorldExporter.getApp().getWorld().getCurrentDimensions()))
				dimensionChooser.setSelectedItem(MCWorldExporter.getApp().getWorld().getCurrentDimensions());
		}

		if (MCWorldExporter.getApp().getExportBounds().getMinX() != ((Integer) minXSpinner.getValue()).intValue()) {
			minXSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getMinX());
		}
		if (MCWorldExporter.getApp().getExportBounds().getMinY() != ((Integer) minYSpinner.getValue()).intValue()) {
			minYSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getMinY());
		}
		if (MCWorldExporter.getApp().getExportBounds().getMinZ() != ((Integer) minZSpinner.getValue()).intValue()) {
			minZSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getMinZ());
		}
		if (MCWorldExporter.getApp().getExportBounds().getMaxX() != ((Integer) maxXSpinner.getValue()).intValue()) {
			maxXSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getMaxX());
		}
		if (MCWorldExporter.getApp().getExportBounds().getMaxY() != ((Integer) maxYSpinner.getValue()).intValue()) {
			maxYSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getMaxY());
		}
		if (MCWorldExporter.getApp().getExportBounds().getMaxZ() != ((Integer) maxZSpinner.getValue()).intValue()) {
			maxZSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getMaxZ());
		}
		if (MCWorldExporter.getApp().getExportBounds().getLodCenterX() != ((Integer) lodCenterXSpinner.getValue()).intValue()) {
			lodCenterXSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getLodCenterX());
		}
		if (MCWorldExporter.getApp().getExportBounds().getLodCenterZ() != ((Integer) lodCenterZSpinner.getValue()).intValue()) {
			lodCenterZSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getLodCenterZ());
		}
		if (MCWorldExporter.getApp().getExportBounds().getLodWidth() != ((Integer) lodWidthSpinner.getValue()).intValue()) {
			lodWidthSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getLodWidth());
		}
		if (MCWorldExporter.getApp().getExportBounds().getLodDepth() != ((Integer) lodDepthSpinner.getValue()).intValue()) {
			lodDepthSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getLodDepth());
		}
		if (MCWorldExporter.getApp().getExportBounds().getLodYDetail() != ((Integer) lodYDetailSpinner.getValue()).intValue()) {
			lodYDetailSpinner.setValue(MCWorldExporter.getApp().getExportBounds().getLodYDetail());
		}
		if (MCWorldExporter.getApp().getExportBounds().hasLod() != lodEnableCheckBox.isSelected()) {
			lodEnableCheckBox.setSelected(MCWorldExporter.getApp().getExportBounds().hasLod());
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
		if (Config.onlyIndividualBlocks != exportIndividualBlocksCheckBox.isSelected()) {
			exportIndividualBlocksCheckBox.setSelected(Config.onlyIndividualBlocks);
		}
		if (Config.chunkSize != ((Integer) chunkSizeSpinner.getValue()).intValue()) {
			chunkSizeSpinner.setValue(Config.chunkSize);
		}
		
		if(prevWorld != MCWorldExporter.getApp().getWorld() && MCWorldExporter.getApp().getWorld() != null) {
			synchronized(prevYOffsetMutex) {
				prevYOffset = 64;
				yOffsetSpinner.setValue(64);
			}
			yOffsetAutoCheckBox.setSelected(true);
		}
		
		if(yOffsetAutoCheckBox.isSelected()) {
			if(MCWorldExporter.getApp().getWorld() == null) {
				yOffsetSpinner.setBorder(new EmptyBorder(1,1,1,1));
			}else {
				int centerX = MCWorldExporter.getApp().getExportBounds().getCenterX();
				int centerZ = MCWorldExporter.getApp().getExportBounds().getCenterZ();
				if(MCWorldExporter.getApp().getExportBounds().hasLod()) {
					centerX = MCWorldExporter.getApp().getExportBounds().getLodCenterX();
					centerZ = MCWorldExporter.getApp().getExportBounds().getLodCenterZ();
				}
				if(centerX != prevCenterX || centerZ != prevCenterZ) {
					prevCenterX = centerX;
					prevCenterZ = centerZ;
					
					yOffsetSpinner.setBorder(new LineBorder(new Color(200, 96, 96)));
					
					final int fcenterX = centerX;
					final int fcenterZ = centerZ;
					Runnable backgroundTask = new Runnable() {
						
						@Override
						public void run() {
							boolean hasNewValue = false;
							int yOffset = 64;
							if(MCWorldExporter.getApp().getWorld() != null) {
								try {
									Chunk chunk = MCWorldExporter.getApp().getWorld().getChunkFromBlockPosition(fcenterX, fcenterZ);
									if(chunk != null) {
										yOffset = chunk.getHeight(fcenterX, fcenterZ) + 1;
										hasNewValue = true;
									}
								}catch(Exception ex) {
								}
							}
							if(hasNewValue) {
								yOffsetSpinner.setBorder(new EmptyBorder(1,1,1,1));
								synchronized(prevYOffsetMutex) {
									if(prevYOffset != yOffset) {
										prevYOffset = yOffset;
										yOffsetSpinner.setValue(yOffset);
									}
								}
							}else {
								yOffsetSpinner.setBorder(new LineBorder(new Color(200, 96, 96)));
							}
						}
						
					};
					BackgroundThread.runInBackground(backgroundTask);
				}
			}
		}else {
			yOffsetSpinner.setBorder(new EmptyBorder(1,1,1,1));
		}
		
		prevWorld = MCWorldExporter.getApp().getWorld();
	}

	public boolean isEditingFGChunks() {
		return editFGChunksButton.isSelected();
	}
	
	private boolean isHoveringOverAbout = false;
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if(isHoveringOverAbout)
			g.setColor(new Color(0.05f, 0.15f, 0.85f));
		else
			g.setColor(getForeground());
		g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() * 1.15f));
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds("About", g);
		g.drawString("About", getWidth() - ((int) stringBounds.getWidth()) - 6, ((int) stringBounds.getHeight()) + 2);
	}
	
	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		super.processMouseMotionEvent(e);
		
		Rectangle2D stringBounds = getGraphics().getFontMetrics().getStringBounds("About", getGraphics());
		int minX = getWidth() - ((int) (stringBounds.getWidth() * 1.15f)) - 20;
		int maxY = ((int) (stringBounds.getHeight() * 1.15f)) + 15;
		if(e.getX() >= minX && e.getY() <= maxY) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			if(!isHoveringOverAbout) {
				isHoveringOverAbout = true;
				repaint();
			}
		}else {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			if(isHoveringOverAbout) {
				isHoveringOverAbout = false;
				repaint();
			}
		}
	}
	
	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if(e.getID() == MouseEvent.MOUSE_CLICKED && e.getButton() == MouseEvent.BUTTON1) {
			Rectangle2D stringBounds = getGraphics().getFontMetrics().getStringBounds("About", getGraphics());
			int minX = getWidth() - ((int) (stringBounds.getWidth() * 1.15f)) - 20;
			int maxY = ((int) (stringBounds.getHeight() * 1.15f)) + 15;
			if(e.getX() >= minX && e.getY() <= maxY) {
				aboutDialog.setVisible(true);
				aboutDialog.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
			}
		}
	}
	
	private void setEnabled(Component comp, boolean enabled) {
		if(comp != this)
			comp.setEnabled(enabled);
		if(comp.getClass().equals(JPanel.class) || comp == this) {
			int num = ((JPanel) comp).getComponentCount();
			for(int i = 0; i < num; ++i) {
				setEnabled(((JPanel) comp).getComponent(i), enabled);
			}
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		setEnabled(this, enabled);
	}

}
