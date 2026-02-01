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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
	private JButton loadSettingsFromExportButton;
	private JToggleButton pauseLoadingButton;
	private JComboBox<String> dimensionChooser;

	private JSpinner minXSpinner;
	private JSpinner minYSpinner;
	private JSpinner minZSpinner;
	private JSpinner maxXSpinner;
	private JSpinner maxYSpinner;
	private JSpinner maxZSpinner;
	private JSpinner originXSpinner;
	private JSpinner originYSpinner;
	private JSpinner originZSpinner;
	private JCheckBox originAutoCheckBox;
	private Object prevOriginMutex = new Object();
	private int prevOriginX;
	private int prevOriginY;
	private int prevOriginZ;
	private World prevWorld;
	private JSpinner chunkSizeSpinner;

	private JButton teleportButton;
	private JButton zoomOutButton;
	private JButton zoomInButton;

	private JToggleButton editFGChunksButton;
	
	private JToggleButton advancedSettingsButton;

	private JButton exportButton;
	private JButton reexportButton;
	
	private File exportLastDirectory;
	
	private WorldBrowser worldBrowser;
	
	private AboutDialog aboutDialog;

	public ToolBar() {
		super();
		
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
		worldPanel.setMinimumSize(new Dimension(320, 140));
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
		loadWorldButton.setPreferredSize(new Dimension(150, 24));
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
		loadWorldFromExportButton.setPreferredSize(new Dimension(150, 24));
		loadWorldFromExportButton.setMinimumSize(loadWorldFromExportButton.getPreferredSize());
		loadWorldFromExportButton.setMaximumSize(loadWorldFromExportButton.getPreferredSize());
		ToolTips.registerTooltip(loadWorldFromExportButton, ToolTips.LOAD_WORLD_FROM_EXPORT);
		loadButtonsPanel.add(loadWorldFromExportButton);
		
		JPanel loadButtonsPadding2 = new JPanel();
		loadButtonsPadding2.setPreferredSize(new Dimension(10, 4));
		loadButtonsPadding2.setMinimumSize(new Dimension(10, 4));
		loadButtonsPadding2.setMaximumSize(new Dimension(10, 4));
		loadButtonsPadding2.setBorder(new EmptyBorder(0,0,0,0));
		loadButtonsPanel.add(loadButtonsPadding2);
		
		loadSettingsFromExportButton = new JButton("Load Settings From Export");
		loadSettingsFromExportButton.setMargin(new Insets(0, 3, 0, 3));
		loadSettingsFromExportButton.setPreferredSize(new Dimension(150, 24));
		loadSettingsFromExportButton.setMinimumSize(loadSettingsFromExportButton.getPreferredSize());
		loadSettingsFromExportButton.setMaximumSize(loadSettingsFromExportButton.getPreferredSize());
		ToolTips.registerTooltip(loadSettingsFromExportButton, ToolTips.LOAD_SETTINGS_FROM_EXPORT);
		loadButtonsPanel.add(loadSettingsFromExportButton);
		
		JPanel loadButtonsPadding3 = new JPanel();
		loadButtonsPadding3.setPreferredSize(new Dimension(10, 8));
		loadButtonsPadding3.setMinimumSize(new Dimension(10, 8));
		loadButtonsPadding3.setMaximumSize(new Dimension(10, 8));
		loadButtonsPadding3.setBorder(new EmptyBorder(0,0,0,0));
		loadButtonsPanel.add(loadButtonsPadding3);
		
		pauseLoadingButton = new JToggleButton("Pause Loading");
		pauseLoadingButton.setPreferredSize(new Dimension(150, 24));
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
		selectionPanel.setMinimumSize(new Dimension(342, 140));
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
		
		selectionCtrlPanel.add(new JPanel());
		
		JPanel selectionOriginPanel = new JPanel();
		selectionOriginPanel.setLayout(new BoxLayout(selectionOriginPanel, BoxLayout.Y_AXIS));
		selectionOriginPanel.setPreferredSize(new Dimension(96, 80));
		selectionOriginPanel.setMinimumSize(selectionOriginPanel.getPreferredSize());
		selectionOriginPanel.setMaximumSize(selectionOriginPanel.getPreferredSize());
		ToolTips.registerTooltip(selectionOriginPanel, ToolTips.EXPORT_ORIGIN);
		JLabel originLabel = new JLabel("Origin");
		originLabel.setBorder(null);
		originLabel.setAlignmentX(0.5f);
		originLabel.setHorizontalAlignment(SwingConstants.CENTER);
		originLabel.setPreferredSize(new Dimension(96, 20));
		originLabel.setMinimumSize(originLabel.getPreferredSize());
		originLabel.setMaximumSize(originLabel.getPreferredSize());
		selectionOriginPanel.add(originLabel);
		originXSpinner = new JSpinner();
		originXSpinner.setPreferredSize(new Dimension(96, 20));
		originXSpinner.setMinimumSize(originXSpinner.getPreferredSize());
		originXSpinner.setMaximumSize(originXSpinner.getPreferredSize());
		originXSpinner.setBorder(new EmptyBorder(1,1,1,1));
		selectionOriginPanel.add(originXSpinner);
		originYSpinner = new JSpinner();
		originYSpinner.setPreferredSize(new Dimension(96, 20));
		originYSpinner.setMinimumSize(originYSpinner.getPreferredSize());
		originYSpinner.setMaximumSize(originYSpinner.getPreferredSize());
		originYSpinner.setBorder(new EmptyBorder(1,1,1,1));
		selectionOriginPanel.add(originYSpinner);
		originZSpinner = new JSpinner();
		originZSpinner.setPreferredSize(new Dimension(96, 20));
		originZSpinner.setMinimumSize(originZSpinner.getPreferredSize());
		originZSpinner.setMaximumSize(originZSpinner.getPreferredSize());
		originZSpinner.setBorder(new EmptyBorder(1,1,1,1));
		selectionOriginPanel.add(originZSpinner);
		selectionCtrlPanel.add(selectionOriginPanel);

		selectionPanel.add(selectionCtrlPanel);
		
		JPanel chunkSizePanel = new JPanel();
		chunkSizePanel.setLayout(new BoxLayout(chunkSizePanel, BoxLayout.X_AXIS));
		chunkSizePanel.setBorder(new EmptyBorder(9, 0, 0, 0));
		ToolTips.registerTooltip(chunkSizePanel, ToolTips.CHUNK_SIZE);
		JLabel chunkSizeLabel = new JLabel("Chunk Size:  ");
		chunkSizePanel.add(chunkSizeLabel);
		chunkSizeSpinner = new JSpinner();
		chunkSizeSpinner.setPreferredSize(new Dimension(64, 20));
		chunkSizeSpinner.setMinimumSize(chunkSizeSpinner.getPreferredSize());
		chunkSizeSpinner.setMaximumSize(chunkSizeSpinner.getPreferredSize());
		((SpinnerNumberModel)(chunkSizeSpinner.getModel())).setMinimum(1);
		((SpinnerNumberModel)(chunkSizeSpinner.getModel())).setMaximum(1024);
		chunkSizeSpinner.setValue(Config.chunkSize);
		chunkSizePanel.add(chunkSizeSpinner);
		
		JPanel chunkSizeSeparator = new JPanel();
		chunkSizeSeparator.setPreferredSize(new Dimension(110, 1));
		chunkSizeSeparator.setMinimumSize(chunkSizeSeparator.getPreferredSize());
		chunkSizeSeparator.setMaximumSize(chunkSizeSeparator.getPreferredSize());
		chunkSizePanel.add(chunkSizeSeparator);
		
		originAutoCheckBox = new JCheckBox("auto");
		originAutoCheckBox.setSelected(true);
		ToolTips.registerTooltip(originAutoCheckBox, ToolTips.EXPORT_ORIGIN_AUTO);
		chunkSizePanel.add(originAutoCheckBox);
		
		selectionPanel.add(chunkSizePanel);

		selectionPanel.add(new JPanel());
		add(selectionPanel);

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
		
		JPanel advancedSettingsPanel = new JPanel();
		advancedSettingsPanel.setLayout(new BoxLayout(advancedSettingsPanel, BoxLayout.Y_AXIS));
		advancedSettingsPanel.setMinimumSize(new Dimension(90, 140));
		advancedSettingsPanel.setMaximumSize(advancedSettingsPanel.getMinimumSize());
		advancedSettingsPanel.setPreferredSize(advancedSettingsPanel.getMinimumSize());
		advancedSettingsPanel.add(new JLabel(" "));
		advancedSettingsPanel.add(new JPanel());
		advancedSettingsButton = new JToggleButton("<html><div text-align:center>Advanced<br>Settings</div></html>");
		advancedSettingsButton.setPreferredSize(new Dimension(84, 84));
		advancedSettingsButton.setMinimumSize(advancedSettingsButton.getPreferredSize());
		advancedSettingsButton.setMaximumSize(advancedSettingsButton.getPreferredSize());
		advancedSettingsButton.setFocusable(false);
		ToolTips.registerTooltip(advancedSettingsButton, ToolTips.ADVANCED_SETTINGS);
		advancedSettingsPanel.add(advancedSettingsButton);
		advancedSettingsPanel.add(new JPanel());
		add(advancedSettingsPanel);

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
		reexportButton = new JButton("Re-export");
		reexportButton.setPreferredSize(new Dimension(84, 40));
		reexportButton.setMinimumSize(reexportButton.getPreferredSize());
		reexportButton.setMaximumSize(reexportButton.getPreferredSize());
		reexportButton.setVisible(false);
		ToolTips.registerTooltip(reexportButton, ToolTips.REEXPORT);
		exportPanel.add(reexportButton);
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
					if(exportData != null) {
						MCWorldExporter.getApp().setLastExportFileOpened(file);
						exportData.apply(false);
					}
				}
			}

		});
		
		loadSettingsFromExportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setApproveButtonText("Load");
				chooser.setDialogTitle("Load Settings From Export");
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
					if(exportData != null) {
						exportData.apply(true);
					}
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
					exportButton.setEnabled(false);
					reexportButton.setEnabled(false);
				}else {
					exportButton.setEnabled(true);
					reexportButton.setEnabled(true);
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
				MCWorldExporter.getApp().getActiveExportBounds().setMinX((Integer) minXSpinner.getValue());
			}

		});

		minYSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setMinY((Integer) minYSpinner.getValue());
				MCWorldExporter.getApp().getUI().fullReRender();
			}

		});

		minZSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setMinZ((Integer) minZSpinner.getValue());
			}

		});

		maxXSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setMaxX((Integer) maxXSpinner.getValue());
			}

		});

		maxYSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setMaxY((Integer) maxYSpinner.getValue());
				MCWorldExporter.getApp().getUI().fullReRender();
			}

		});

		maxZSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getActiveExportBounds().setMaxZ((Integer) maxZSpinner.getValue());
			}

		});
		
		originXSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBoundsList().get(0).setOffsetX((Integer) originXSpinner.getValue());
				synchronized(prevOriginMutex) {
					if(originAutoCheckBox.isSelected() && ((Integer) originXSpinner.getValue()) != prevOriginX) {
						// If we're in auto mode, but the value in the spinner doesn't match
						// the value in the prevYOffset, then the user manually changed it.
						// So, we need to turn auto off.
						originAutoCheckBox.setSelected(false);
					}
				}
			}
			
		});
		
		originYSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBoundsList().get(0).setOffsetY((Integer) originYSpinner.getValue());
				synchronized(prevOriginMutex) {
					if(originAutoCheckBox.isSelected() && ((Integer) originYSpinner.getValue()) != prevOriginY) {
						// If we're in auto mode, but the value in the spinner doesn't match
						// the value in the prevYOffset, then the user manually changed it.
						// So, we need to turn auto off.
						originAutoCheckBox.setSelected(false);
					}
				}
			}
			
		});
		
		originZSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				MCWorldExporter.getApp().getExportBoundsList().get(0).setOffsetZ((Integer) originZSpinner.getValue());
				synchronized(prevOriginMutex) {
					if(originAutoCheckBox.isSelected() && ((Integer) originZSpinner.getValue()) != prevOriginZ) {
						// If we're in auto mode, but the value in the spinner doesn't match
						// the value in the prevYOffset, then the user manually changed it.
						// So, we need to turn auto off.
						originAutoCheckBox.setSelected(false);
					}
				}
			}
			
		});
		
		originAutoCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				update();
			}
			
		});
		
		chunkSizeSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				int chunkSize = ((Integer) chunkSizeSpinner.getValue()).intValue();
				MCWorldExporter.getApp().getActiveExportBounds().setChunkSize(chunkSize);
				if(MCWorldExporter.getApp().getActiveExportBoundsIndex() == 0) {
					Config.chunkSize = chunkSize;
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
		
		Border advancedSettingsButtonBorder = advancedSettingsButton.getBorder();
		advancedSettingsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MCWorldExporter.getApp().getUI().toggleAdvancedSettings(advancedSettingsButton.isSelected());
				if(advancedSettingsButton.isSelected()) {
					advancedSettingsButton.setBorder(new LineBorder(new Color(0f,0.7f,1f), 4));
				}else {
					advancedSettingsButton.setBorder(advancedSettingsButtonBorder);
				}
			}

		});
		
		exportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
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
		
		reexportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(MCWorldExporter.getApp().getLastExportFileOpened() != null) {
					File file = MCWorldExporter.getApp().getLastExportFileOpened();
					
					try {
						exportLastDirectory = file.getParentFile();
						
						Exporter.export(file);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}else {
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
			}

		});
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {}
		});
	}
	
	public void update() {
		if (MCWorldExporter.getApp().getWorld() == null) {
			dimensionChooser.removeAllItems();
		} else {
			if(pauseLoadingButton.isSelected() != MCWorldExporter.getApp().getWorld().isPaused()) {
				pauseLoadingButton.setSelected(MCWorldExporter.getApp().getWorld().isPaused());
				if(pauseLoadingButton.isSelected()) {
					exportButton.setEnabled(false);
					reexportButton.setEnabled(false);
				}else {
					exportButton.setEnabled(true);
					reexportButton.setEnabled(true);
				}
			}
			
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

		if (MCWorldExporter.getApp().getActiveExportBounds().getMinX() != ((Integer) minXSpinner.getValue()).intValue()) {
			minXSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getMinX());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getMinY() != ((Integer) minYSpinner.getValue()).intValue()) {
			minYSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getMinY());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getMinZ() != ((Integer) minZSpinner.getValue()).intValue()) {
			minZSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getMinZ());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getMaxX() != ((Integer) maxXSpinner.getValue()).intValue()) {
			maxXSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getMaxX());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getMaxY() != ((Integer) maxYSpinner.getValue()).intValue()) {
			maxYSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getMaxY());
		}
		if (MCWorldExporter.getApp().getActiveExportBounds().getMaxZ() != ((Integer) maxZSpinner.getValue()).intValue()) {
			maxZSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getMaxZ());
		}
		
		if(prevWorld != MCWorldExporter.getApp().getWorld() && MCWorldExporter.getApp().getWorld() != null) {
			synchronized(prevOriginMutex) {
				prevOriginX = 0;
				originXSpinner.setValue(0);
				prevOriginY = 64;
				originYSpinner.setValue(64);
				prevOriginZ = 0;
				originZSpinner.setValue(0);
			}
			originAutoCheckBox.setSelected(true);
		}
		prevWorld = MCWorldExporter.getApp().getWorld();
		
		if(originAutoCheckBox.isSelected()) {
			if(MCWorldExporter.getApp().getWorld() == null) {
				originYSpinner.setBorder(new EmptyBorder(1,1,1,1));
			}else {
				int centerX = MCWorldExporter.getApp().getExportBoundsList().get(0).getCenterX();
				int centerZ = MCWorldExporter.getApp().getExportBoundsList().get(0).getCenterZ();
				if(MCWorldExporter.getApp().getActiveExportBounds().hasLod()) {
					centerX = MCWorldExporter.getApp().getExportBoundsList().get(0).getLodCenterX();
					centerZ = MCWorldExporter.getApp().getExportBoundsList().get(0).getLodCenterZ();
				}
				if(centerX != prevOriginX || centerZ != prevOriginZ) {
					
					originYSpinner.setBorder(new LineBorder(new Color(200, 96, 96)));
					prevOriginX = centerX;
					originXSpinner.setValue(centerX);
					prevOriginZ = centerZ;
					originZSpinner.setValue(centerZ);
					
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
								originYSpinner.setBorder(new EmptyBorder(1,1,1,1));
								synchronized(prevOriginMutex) {
									if(prevOriginY != yOffset) {
										prevOriginY = yOffset;
										originYSpinner.setValue(yOffset);
									}
								}
							}else {
								originYSpinner.setBorder(new LineBorder(new Color(200, 96, 96)));
							}
						}
						
					};
					BackgroundThread.runInBackground(backgroundTask);
				}
			}
		}else {
			originYSpinner.setBorder(new EmptyBorder(1,1,1,1));
		}
		
		if (MCWorldExporter.getApp().getActiveExportBounds().getChunkSize() != ((Integer) chunkSizeSpinner.getValue()).intValue()) {
			chunkSizeSpinner.setValue(MCWorldExporter.getApp().getActiveExportBounds().getChunkSize());
		}
		
		if(MCWorldExporter.getApp().getLastExportFileOpened() == null) {
			reexportButton.setVisible(false);
			exportButton.setPreferredSize(new Dimension(84, 84));
			exportButton.setMinimumSize(exportButton.getPreferredSize());
			exportButton.setMaximumSize(exportButton.getPreferredSize());
		}else {
			reexportButton.setVisible(true);
			exportButton.setPreferredSize(new Dimension(84, 40));
			exportButton.setMinimumSize(exportButton.getPreferredSize());
			exportButton.setMaximumSize(exportButton.getPreferredSize());
		}
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
				aboutDialog.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
				aboutDialog.setVisible(true);
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
