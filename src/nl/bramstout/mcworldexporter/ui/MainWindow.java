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
import java.awt.Graphics;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.ReleaseChecker;

public class MainWindow extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JLayeredPane rootPanel;
	private JPanel contentPanel;
	private JPanel loadingPanel;
	private JPanel topPanel;
	private ReleasePanel releasePanel;
	private ToolBar toolbar;
	private WorldViewer2D viewer;
	private ProgressBar progressBar;
	private ResourcePackManager resourcePackManager;
	private EntityDialog entityDialog;
	
	public MainWindow() {
		super();
		entityDialog = new EntityDialog(this);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		rootPanel = new JLayeredPane();
		rootPanel.setLayout(new OverlayLayout(rootPanel));
		rootPanel.setBorder(new EmptyBorder(0,0,0,0));
		add(rootPanel);
		
		contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		rootPanel.add(contentPanel, Integer.valueOf(0), 0);
		
		loadingPanel = new JPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(new Color(
						(getBackground().getRed() * 15) / 16,
						(getBackground().getGreen() * 15) / 16,
						(getBackground().getBlue() * 15) / 16,
						170));
				g.fillRect(0, 0, getWidth(), getHeight());
			}
			
		};
		loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.X_AXIS));
		loadingPanel.setOpaque(false);
		rootPanel.add(loadingPanel, Integer.valueOf(1), 0);
		JLabel loadingLabel = new JLabel("Loading");
		loadingLabel.setFont(loadingLabel.getFont().deriveFont(32.0f));
		loadingLabel.setHorizontalTextPosition(JLabel.CENTER);
		JPanel paddingPanel1 = new JPanel();
		paddingPanel1.setOpaque(false);
		JPanel paddingPanel2 = new JPanel();
		paddingPanel2.setOpaque(false);
		loadingPanel.add(paddingPanel1);
		loadingPanel.add(loadingLabel);
		loadingPanel.add(paddingPanel2);
		
		topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBorder(new EmptyBorder(0,0,0,0));
		contentPanel.add(topPanel, BorderLayout.NORTH);
		
		releasePanel = new ReleasePanel();
		topPanel.add(releasePanel);
		
		toolbar = new ToolBar();
		topPanel.add(toolbar);
		
		viewer = new WorldViewer2D();
		contentPanel.add(viewer, BorderLayout.CENTER);
		
		progressBar = new ProgressBar();
		contentPanel.add(progressBar, BorderLayout.SOUTH);
		
		resourcePackManager = new ResourcePackManager();
		contentPanel.add(resourcePackManager, BorderLayout.EAST);
		
		setSize(1400, 800);
		setTitle("");
	}
	
	public void setTitle(String title) {
		title = "MiEx " + ReleaseChecker.CURRENT_VERSION + " | " + title;
		if(MCWorldExporter.getApp().getLastExportFileOpened() != null) {
			title += " | Loaded from " + MCWorldExporter.getApp().getLastExportFileOpened().getName();
		}
		super.setTitle(title);
	}
	
	public void update() {
		//viewer.render();
		//viewer.repaint();
		toolbar.update();
	}
	
	public void fullReRender() {
		viewer.fullReRender();
	}
	
	public void reset() {
		update();
		viewer.reset();
	}
	
	
	@Override
	public void setEnabled(boolean b) {
		if(b)
			super.setEnabled(b);
		toolbar.setEnabled(b);
		resourcePackManager.setEnabled(b);
		entityDialog.setEnabled(b);
		viewer.setEnabled(b);
		loadingPanel.setVisible(!b);
	}
	
	public WorldViewer2D getViewer() {
		return viewer;
	}
	
	public ProgressBar getProgressBar() {
		return progressBar;
	}
	
	public ToolBar getToolbar() {
		return toolbar;
	}
	
	public ResourcePackManager getResourcePackManager() {
		return resourcePackManager;
	}
	
	public EntityDialog getEntityDialog() {
		return entityDialog;
	}

}
