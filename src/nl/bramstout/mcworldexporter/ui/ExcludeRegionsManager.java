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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nl.bramstout.mcworldexporter.ExportBounds.ExcludeRegion;
import nl.bramstout.mcworldexporter.MCWorldExporter;

public class ExcludeRegionsManager extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel regionsPanel;
	
	public ExcludeRegionsManager() {
		ToolTips.registerTooltip(this, ToolTips.EXCLUDE_REGIONS_MANAGER);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JLabel regionsLabel = new JLabel("Exclude Regions");
		regionsLabel.setAlignmentX(0f);
		regionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		regionsLabel.setPreferredSize(new Dimension(300, 20));
		regionsLabel.setMinimumSize(new Dimension(300, 20));
		regionsLabel.setMaximumSize(new Dimension(300, 20));
		add(regionsLabel);
		regionsPanel = new JPanel();
		regionsPanel.setLayout(new BoxLayout(regionsPanel, BoxLayout.Y_AXIS));
		regionsPanel.setBackground(getBackground().brighter());
		JScrollPane regionsScrollPane = new JScrollPane(regionsPanel);
		regionsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		regionsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		regionsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		regionsScrollPane.setPreferredSize(new Dimension(300, 350));
		regionsScrollPane.setAlignmentX(0f);
		add(regionsScrollPane);
		
		JButton addButton = new JButton("+");
		addButton.setPreferredSize(new Dimension(300, 24));
		addButton.setMinimumSize(new Dimension(300, 24));
		addButton.setMaximumSize(new Dimension(300, 24));
		add(addButton);
		
		addButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int minX = MCWorldExporter.getApp().getActiveExportBounds().getMinX();
				int minY = MCWorldExporter.getApp().getActiveExportBounds().getMinY();
				int minZ = MCWorldExporter.getApp().getActiveExportBounds().getMinZ();
				int maxX = MCWorldExporter.getApp().getActiveExportBounds().getMaxX();
				int maxY = MCWorldExporter.getApp().getActiveExportBounds().getMaxY();
				int maxZ = MCWorldExporter.getApp().getActiveExportBounds().getMaxZ();
				int centerX = MCWorldExporter.getApp().getActiveExportBounds().getCenterX();
				int centerZ = MCWorldExporter.getApp().getActiveExportBounds().getCenterZ();
				minX = (minX + centerX + centerX) / 3;
				maxX = (maxX + centerX + centerX) / 3;
				minZ = (minZ + centerZ + centerZ) / 3;
				maxZ = (maxZ + centerZ + centerZ) / 3;
				MCWorldExporter.getApp().getActiveExportBounds().excludeRegions.add(
						new ExcludeRegion(minX, minY, minZ, maxX, maxY, maxZ));
				MCWorldExporter.getApp().getUI().update();
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
		
		regionsPanel.addMouseListener(new MouseListener() {
			
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
		
		update();
	}
	
	public void update() {
		boolean needsUpdate = false;
		if(regionsPanel.getComponentCount() != MCWorldExporter.getApp().getActiveExportBounds().getExcludeRegions().size()) {
			needsUpdate = true;
		}else {
			for(int i = 0; i < regionsPanel.getComponentCount(); ++i) {
				if(((RegionComponent)regionsPanel.getComponent(i)).region != 
						MCWorldExporter.getApp().getActiveExportBounds().getExcludeRegions().get(i)) {
					needsUpdate = true;
					break;
				}
			}
		}
		if(!needsUpdate)
			return;
		regionsPanel.removeAll();
		for(ExcludeRegion region : MCWorldExporter.getApp().getActiveExportBounds().getExcludeRegions()) {
			regionsPanel.add(new RegionComponent(region));
		}
		revalidate();
		repaint();
		regionsPanel.revalidate();
		regionsPanel.repaint();
	}
	
	private static class RegionComponent extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public ExcludeRegion region;
		private JLabel minLabel;
		private JLabel maxLabel;
		private JSpinner minXSpinner;
		private JSpinner minYSpinner;
		private JSpinner minZSpinner;
		private JSpinner maxXSpinner;
		private JSpinner maxYSpinner;
		private JSpinner maxZSpinner;
		private JButton deleteButton;
		
		public RegionComponent(ExcludeRegion region) {
			this.region = region;
			setLayout(new BorderLayout(4, 0));
			setBorder(new EmptyBorder(4, 4, 4, 4));
			
			JPanel inputPanel = new JPanel();
			inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
			add(inputPanel, BorderLayout.CENTER);
			JPanel minPanel = new JPanel();
			minPanel.setLayout(new BoxLayout(minPanel, BoxLayout.X_AXIS));
			minPanel.setBorder(new EmptyBorder(0, 0, 2, 0));
			inputPanel.add(minPanel);
			JPanel maxPanel = new JPanel();
			maxPanel.setLayout(new BoxLayout(maxPanel, BoxLayout.X_AXIS));
			maxPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
			inputPanel.add(maxPanel);
			
			minLabel = new JLabel("Min ");
			minLabel.setPreferredSize(new Dimension(26, 20));
			minLabel.setMinimumSize(minLabel.getPreferredSize());
			minLabel.setMaximumSize(minLabel.getPreferredSize());
			minPanel.add(minLabel);
			maxLabel = new JLabel("Max ");
			maxLabel.setPreferredSize(new Dimension(26, 20));
			maxLabel.setMinimumSize(maxLabel.getPreferredSize());
			maxLabel.setMaximumSize(maxLabel.getPreferredSize());
			maxPanel.add(maxLabel);
			
			minXSpinner = new JSpinner();
			minXSpinner.setPreferredSize(new Dimension(60, 20));
			minXSpinner.setMinimumSize(minXSpinner.getPreferredSize());
			minXSpinner.setMaximumSize(minXSpinner.getPreferredSize());
			minPanel.add(minXSpinner);
			minYSpinner = new JSpinner();
			minYSpinner.setPreferredSize(new Dimension(60, 20));
			minYSpinner.setMinimumSize(minYSpinner.getPreferredSize());
			minYSpinner.setMaximumSize(minYSpinner.getPreferredSize());
			minPanel.add(minYSpinner);
			minZSpinner = new JSpinner();
			minZSpinner.setPreferredSize(new Dimension(60, 20));
			minZSpinner.setMinimumSize(minZSpinner.getPreferredSize());
			minZSpinner.setMaximumSize(minZSpinner.getPreferredSize());
			minPanel.add(minZSpinner);
			
			maxXSpinner = new JSpinner();
			maxXSpinner.setPreferredSize(new Dimension(60, 20));
			maxXSpinner.setMinimumSize(maxXSpinner.getPreferredSize());
			maxXSpinner.setMaximumSize(maxXSpinner.getPreferredSize());
			maxPanel.add(maxXSpinner);
			maxYSpinner = new JSpinner();
			maxYSpinner.setPreferredSize(new Dimension(60, 20));
			maxYSpinner.setMinimumSize(maxYSpinner.getPreferredSize());
			maxYSpinner.setMaximumSize(maxYSpinner.getPreferredSize());
			maxPanel.add(maxYSpinner);
			maxZSpinner = new JSpinner();
			maxZSpinner.setPreferredSize(new Dimension(60, 20));
			maxZSpinner.setMinimumSize(maxZSpinner.getPreferredSize());
			maxZSpinner.setMaximumSize(maxZSpinner.getPreferredSize());
			maxPanel.add(maxZSpinner);
			
			this.deleteButton = new JButton("X");
			add(this.deleteButton, BorderLayout.EAST);
			this.deleteButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					MCWorldExporter.getApp().getActiveExportBounds().excludeRegions.remove(region);
					MCWorldExporter.getApp().getUI().update();
				}
			});
			
			setPreferredSize(new Dimension(250, 50));
			setMinimumSize(new Dimension(230, 50));
			setMaximumSize(new Dimension(300, 50));
			
			minXSpinner.setValue(region.minX);
			minYSpinner.setValue(region.minY);
			minZSpinner.setValue(region.minZ);
			maxXSpinner.setValue(region.maxX);
			maxYSpinner.setValue(region.maxY);
			maxZSpinner.setValue(region.maxZ);
			
			minXSpinner.addChangeListener(new ChangeListener() {				
				@Override
				public void stateChanged(ChangeEvent e) {
					region.minX = ((Integer) minXSpinner.getValue()).intValue();
					if(region.minX > region.maxX) {
						region.maxX = region.minX;
						maxXSpinner.setValue(region.maxX);
					}
				}
			});
			minYSpinner.addChangeListener(new ChangeListener() {				
				@Override
				public void stateChanged(ChangeEvent e) {
					region.minY = ((Integer) minYSpinner.getValue()).intValue();
					if(region.minY > region.maxY) {
						region.maxY = region.minY;
						maxYSpinner.setValue(region.maxY);
					}
				}
			});
			minZSpinner.addChangeListener(new ChangeListener() {				
				@Override
				public void stateChanged(ChangeEvent e) {
					region.minZ = ((Integer) minZSpinner.getValue()).intValue();
					if(region.minZ > region.maxZ) {
						region.maxZ = region.minZ;
						maxZSpinner.setValue(region.maxZ);
					}
				}
			});
			maxXSpinner.addChangeListener(new ChangeListener() {				
				@Override
				public void stateChanged(ChangeEvent e) {
					region.maxX = ((Integer) maxXSpinner.getValue()).intValue();
					if(region.maxX < region.minX) {
						region.minX = region.maxX;
						minXSpinner.setValue(region.minX);
					}
				}
			});
			maxYSpinner.addChangeListener(new ChangeListener() {				
				@Override
				public void stateChanged(ChangeEvent e) {
					region.maxY = ((Integer) maxYSpinner.getValue()).intValue();
					if(region.maxY < region.minY) {
						region.minY = region.maxY;
						minYSpinner.setValue(region.minY);
					}
				}
			});
			maxZSpinner.addChangeListener(new ChangeListener() {				
				@Override
				public void stateChanged(ChangeEvent e) {
					region.maxZ = ((Integer) maxZSpinner.getValue()).intValue();
					if(region.maxZ < region.minZ) {
						region.minZ = region.maxZ;
						minZSpinner.setValue(region.minZ);
					}
				}
			});
		}
		
	}

}
