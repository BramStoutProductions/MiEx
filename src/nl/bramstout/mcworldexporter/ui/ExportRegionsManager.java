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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.MCWorldExporter;

public class ExportRegionsManager extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel regionsPanel;
	
	public ExportRegionsManager() {
		ToolTips.registerTooltip(this, ToolTips.EXPORT_REGIONS_MANAGER);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JLabel regionsLabel = new JLabel("Export Regions");
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
		regionsScrollPane.setPreferredSize(new Dimension(300, 200));
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
				MCWorldExporter.getApp().addExportBounds();
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
		if(regionsPanel.getComponentCount() != MCWorldExporter.getApp().getExportBoundsList().size()) {
			needsUpdate = true;
		}else {
			for(int i = 0; i < regionsPanel.getComponentCount(); ++i) {
				if(((RegionComponent)regionsPanel.getComponent(i)).region != 
						MCWorldExporter.getApp().getExportBoundsList().get(i)) {
					needsUpdate = true;
				}
			}
		}
		if(!needsUpdate) {
			for(int i = 0; i < regionsPanel.getComponentCount(); ++i) {
				((RegionComponent)regionsPanel.getComponent(i)).update();
			}
			return;
		}
		regionsPanel.removeAll();
		for(ExportBounds region : MCWorldExporter.getApp().getExportBoundsList()) {
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
		public ExportBounds region;
		private JLabel label;
		private JTextField renameField;
		private JButton deleteButton;
		private Color defaultBackground;
		
		public RegionComponent(ExportBounds region) {
			this.region = region;
			setLayout(new BorderLayout(8, 0));
			setBorder(new EmptyBorder(0, 8, 0, 0));
			
			this.label = new JLabel(region.getName());
			add(this.label, BorderLayout.CENTER);
			
			this.renameField = new JTextField(region.getName());
			
			this.deleteButton = new JButton("X");
			add(this.deleteButton, BorderLayout.EAST);
			this.deleteButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					MCWorldExporter.getApp().deleteExportBounds(region.getName());
				}
			});
			
			setPreferredSize(new Dimension(250, 22));
			setMinimumSize(new Dimension(230, 22));
			setMaximumSize(new Dimension(300, 22));
			
			defaultBackground = getBackground();
			if(MCWorldExporter.getApp().getActiveExportBounds().getName().equals(region.getName())) {
				setBackground(new Color(0f,0.7f,1f));
			}
			
			JPopupMenu contextMenu = new JPopupMenu("");
			JMenuItem renameButton = contextMenu.add("Rename");
			
			setComponentPopupMenu(contextMenu);
			this.label.setInheritsPopupMenu(true);
			
			addMouseListener(new MouseListener() {
				
				@Override
				public void mouseReleased(MouseEvent e) {}
				
				@Override
				public void mousePressed(MouseEvent e) {
					if(e.getButton() == MouseEvent.BUTTON1) {
						MCWorldExporter.getApp().setActiveExportBounds(region.getName());
						revalidate();
						repaint();
					}
				}
				
				@Override
				public void mouseExited(MouseEvent e) {}
				
				@Override
				public void mouseEntered(MouseEvent e) {}
				
				@Override
				public void mouseClicked(MouseEvent e) {}
			});
			
			renameButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					remove(label);
					add(renameField, BorderLayout.CENTER);
					revalidate();
					repaint();
					renameField.selectAll();
					renameField.grabFocus();
				}
			});
			
			renameField.addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					remove(renameField);
					add(label, BorderLayout.CENTER);
					revalidate();
					repaint();
				}
				
				@Override
				public void focusGained(FocusEvent e) {}
			});
			
			renameField.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if(renameField.getText() != null) {
						region.setName(renameField.getText());
						label.setText(region.getName());
						renameField.setText(region.getName());
					}
					remove(renameField);
					add(label, BorderLayout.CENTER);
					revalidate();
					repaint();
				}
			});
		}
		
		public void update() {
			if(MCWorldExporter.getApp().getActiveExportBounds().getName().equals(region.getName())) {
				setBackground(new Color(0f,0.7f,1f));
			}else {
				setBackground(defaultBackground);
			}
			revalidate();
			repaint();
		}
		
	}

}
