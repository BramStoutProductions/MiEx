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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import nl.bramstout.mcworldexporter.export.Exporter;

public class ToolBar extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JButton loadWorldButton;
	private JComboBox<String> dimensionChooser;

	private JSpinner minXSpinner;
	private JSpinner minYSpinner;
	private JSpinner minZSpinner;
	private JSpinner maxXSpinner;
	private JSpinner maxYSpinner;
	private JSpinner maxZSpinner;
	
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

	private JButton exportButton;
	
	private File exportLastDirectory;

	public ToolBar() {
		super();
		
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
		worldPanel.setMinimumSize(new Dimension(250, 140));
		worldPanel.setMaximumSize(worldPanel.getMinimumSize());
		worldPanel.setPreferredSize(worldPanel.getMinimumSize());
		JLabel worldPanelLabel = new JLabel("World");
		worldPanelLabel.setAlignmentX(0.5f);
		worldPanel.add(worldPanelLabel);
		worldPanel.add(new JPanel());
		JPanel worldCtrlPanel = new JPanel();
		worldCtrlPanel.setLayout(new BoxLayout(worldCtrlPanel, BoxLayout.X_AXIS));
		loadWorldButton = new JButton("Load");
		loadWorldButton.setPreferredSize(new Dimension(80, 24));
		loadWorldButton.setMinimumSize(loadWorldButton.getPreferredSize());
		loadWorldButton.setMaximumSize(loadWorldButton.getPreferredSize());
		worldCtrlPanel.add(loadWorldButton);
		worldCtrlPanel.add(new JPanel());
		dimensionChooser = new JComboBox<String>();
		dimensionChooser.setPreferredSize(new Dimension(150, 24));
		dimensionChooser.setMinimumSize(dimensionChooser.getPreferredSize());
		dimensionChooser.setMaximumSize(dimensionChooser.getPreferredSize());
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

		selectionPanel.add(new JPanel());
		add(selectionPanel);

		add(new JPanel());
		
		JPanel lodPanel = new JPanel();
		lodPanel.setLayout(new BoxLayout(lodPanel, BoxLayout.Y_AXIS));
		lodPanel.setMinimumSize(new Dimension(144, 140));
		lodPanel.setMaximumSize(selectionPanel.getMinimumSize());
		lodPanel.setPreferredSize(selectionPanel.getMinimumSize());
		lodEnableCheckBox = new JCheckBox("LOD");
		lodEnableCheckBox.setSelected(false);
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
		zoomCtrlPanel.add(zoomOutButton);
		zoomInButton = new JButton("+");
		zoomInButton.setPreferredSize(new Dimension(48, 48));
		zoomInButton.setMinimumSize(zoomInButton.getPreferredSize());
		zoomInButton.setMaximumSize(zoomInButton.getPreferredSize());
		zoomCtrlPanel.add(zoomInButton);
		teleportButton = new JButton("TP");
		teleportButton.setPreferredSize(new Dimension(48, 48));
		teleportButton.setMinimumSize(teleportButton.getPreferredSize());
		teleportButton.setMaximumSize(teleportButton.getPreferredSize());
		zoomCtrlPanel.add(teleportButton);
		zoomPanel.add(zoomCtrlPanel);
		zoomPanel.add(new JPanel());
		add(zoomPanel);

		add(new JPanel());

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
		settingsPanel.setMinimumSize(new Dimension(160, 140));
		settingsPanel.setMaximumSize(settingsPanel.getMinimumSize());
		settingsPanel.setPreferredSize(settingsPanel.getMinimumSize());
		settingsPanel.add(new JLabel(" "));
		settingsPanel.add(new JPanel());
		runOptimiserCheckBox = new JCheckBox("Run Optimiser");
		runOptimiserCheckBox.setSelected(Config.runOptimiser);
		runOptimiserCheckBox.setPreferredSize(new Dimension(410, 24));
		runOptimiserCheckBox.setMinimumSize(new Dimension(410, 24));
		runOptimiserCheckBox.setMaximumSize(new Dimension(410, 24));
		settingsPanel.add(runOptimiserCheckBox);
		runOptimiserCheckBox.setAlignmentX(1);
		
		removeCavesCheckBox = new JCheckBox("Remove Caves");
		removeCavesCheckBox.setSelected(Config.removeCaves);
		removeCavesCheckBox.setPreferredSize(new Dimension(410, 24));
		removeCavesCheckBox.setMinimumSize(new Dimension(410, 24));
		removeCavesCheckBox.setMaximumSize(new Dimension(410, 24));
		settingsPanel.add(removeCavesCheckBox);
		removeCavesCheckBox.setAlignmentX(1);
		
		fillInCavesCheckBox = new JCheckBox("Fill In Caves");
		fillInCavesCheckBox.setSelected(Config.fillInCaves);
		fillInCavesCheckBox.setPreferredSize(new Dimension(394, 24));
		fillInCavesCheckBox.setMinimumSize(new Dimension(394, 24));
		fillInCavesCheckBox.setMaximumSize(new Dimension(394, 24));
		fillInCavesCheckBox.setBorder(new EmptyBorder(0, 16, 0, 0));
		settingsPanel.add(fillInCavesCheckBox);
		fillInCavesCheckBox.setAlignmentX(1);

		exportIndividualBlocksCheckBox = new JCheckBox("Individual Blocks");
		exportIndividualBlocksCheckBox.setSelected(Config.onlyIndividualBlocks);
		exportIndividualBlocksCheckBox.setPreferredSize(new Dimension(410, 24));
		exportIndividualBlocksCheckBox.setMinimumSize(new Dimension(410, 24));
		exportIndividualBlocksCheckBox.setMaximumSize(new Dimension(410, 24));
		settingsPanel.add(exportIndividualBlocksCheckBox);
		exportIndividualBlocksCheckBox.setAlignmentX(1);
		
		JPanel chunkSizePanel = new JPanel();
		chunkSizePanel.setLayout(new BoxLayout(chunkSizePanel, BoxLayout.X_AXIS));
		chunkSizePanel.setPreferredSize(new Dimension(410, 20));
		chunkSizePanel.setMinimumSize(chunkSizePanel.getPreferredSize());
		chunkSizePanel.setMaximumSize(chunkSizePanel.getPreferredSize());
		JLabel chunkSizeLabel = new JLabel("Chunk Size:");
		chunkSizeLabel.setPreferredSize(new Dimension(64, 20));
		chunkSizeLabel.setMinimumSize(chunkSizeLabel.getPreferredSize());
		chunkSizeLabel.setMaximumSize(chunkSizeLabel.getPreferredSize());
		chunkSizePanel.add(chunkSizeLabel);
		chunkSizeSpinner = new JSpinner();
		chunkSizeSpinner.setPreferredSize(new Dimension(48, 20));
		chunkSizeSpinner.setValue(Config.chunkSize);
		((SpinnerNumberModel)(chunkSizeSpinner.getModel())).setMinimum(1);
		((SpinnerNumberModel)(chunkSizeSpinner.getModel())).setMaximum(64);
		chunkSizeSpinner.setMinimumSize(chunkSizeSpinner.getPreferredSize());
		chunkSizeSpinner.setMaximumSize(chunkSizeSpinner.getPreferredSize());
		chunkSizePanel.add(chunkSizeSpinner);
		settingsPanel.add(chunkSizePanel);

		settingsPanel.add(new JPanel());
		add(settingsPanel);

		add(new JPanel());

		JPanel editFGChunksPanel = new JPanel();
		editFGChunksPanel.setLayout(new BoxLayout(editFGChunksPanel, BoxLayout.Y_AXIS));
		editFGChunksPanel.setMinimumSize(new Dimension(120, 140));
		editFGChunksPanel.setMaximumSize(editFGChunksPanel.getMinimumSize());
		editFGChunksPanel.setPreferredSize(editFGChunksPanel.getMinimumSize());
		editFGChunksPanel.add(new JLabel(" "));
		editFGChunksPanel.add(new JPanel());
		editFGChunksButton = new JToggleButton("Edit FG");
		editFGChunksButton.setPreferredSize(new Dimension(84, 84));
		editFGChunksButton.setMinimumSize(editFGChunksButton.getPreferredSize());
		editFGChunksButton.setMaximumSize(editFGChunksButton.getPreferredSize());
		editFGChunksButton.setFocusable(false);
		editFGChunksPanel.add(editFGChunksButton);
		editFGChunksPanel.add(new JPanel());
		add(editFGChunksPanel);

		add(new JPanel());

		JPanel exportPanel = new JPanel();
		exportPanel.setLayout(new BoxLayout(exportPanel, BoxLayout.Y_AXIS));
		exportPanel.setMinimumSize(new Dimension(120, 140));
		exportPanel.setMaximumSize(exportPanel.getMinimumSize());
		exportPanel.setPreferredSize(exportPanel.getMinimumSize());
		exportPanel.add(new JLabel(" "));
		exportPanel.add(new JPanel());
		exportButton = new JButton("Export");
		exportButton.setPreferredSize(new Dimension(84, 84));
		exportButton.setMinimumSize(exportButton.getPreferredSize());
		exportButton.setMaximumSize(exportButton.getPreferredSize());
		exportPanel.add(exportButton);
		exportPanel.add(new JPanel());
		add(exportPanel);

		add(new JPanel());
		add(new JPanel());
		add(new JPanel());

		loadWorldButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setApproveButtonText("Load");
				chooser.setDialogTitle("Load World");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (new File(FileUtil.getMinecraftSavesDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getMinecraftSavesDir()));
				chooser.setFileFilter(null);
				chooser.setAcceptAllFileFilterUsed(false);
				int result = chooser.showOpenDialog(MCWorldExporter.getApp().getUI());
				if (result == JFileChooser.APPROVE_OPTION) {
					MCWorldExporter.getApp().setWorld(chooser.getSelectedFile());
				}
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
				if(!lodEnableCheckBox.isSelected()) {
					if(MCWorldExporter.getApp().getExportBounds().hasLod() != lodEnableCheckBox.isSelected()) {
						MCWorldExporter.getApp().getExportBounds().disableLod();
					}
				}else {
					MCWorldExporter.getApp().getExportBounds().setLodWidth(Math.min(1024, 
													MCWorldExporter.getApp().getExportBounds().getWidth()/2));
					MCWorldExporter.getApp().getExportBounds().setLodDepth(Math.min(1024,
													MCWorldExporter.getApp().getExportBounds().getDepth()/2));
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

	public void update() {
		if (MCWorldExporter.getApp().getWorld() == null) {
			dimensionChooser.removeAllItems();
		} else {
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
		if (Config.runOptimiser != runOptimiserCheckBox.isSelected()) {
			runOptimiserCheckBox.setSelected(Config.runOptimiser);
		}
		if (Config.chunkSize != ((Integer) chunkSizeSpinner.getValue()).intValue()) {
			chunkSizeSpinner.setValue(Config.chunkSize);
		}
	}

	public boolean isEditingFGChunks() {
		return editFGChunksButton.isSelected();
	}

}
