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

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MainWindow extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel topPanel;
	private ReleasePanel releasePanel;
	private ToolBar toolbar;
	private WorldViewer2D viewer;
	private ProgressBar progressBar;
	private ResourcePackManager resourcePackManager;
	
	public MainWindow() {
		super();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		
		topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBorder(new EmptyBorder(0,0,0,0));
		add(topPanel, BorderLayout.NORTH);
		
		releasePanel = new ReleasePanel();
		topPanel.add(releasePanel);
		
		toolbar = new ToolBar();
		topPanel.add(toolbar);
		
		viewer = new WorldViewer2D();
		add(viewer, BorderLayout.CENTER);
		
		progressBar = new ProgressBar();
		add(progressBar, BorderLayout.SOUTH);
		
		resourcePackManager = new ResourcePackManager();
		add(resourcePackManager, BorderLayout.EAST);
		
		setSize(1400, 800);
		setTitle("");
	}
	
	public void setTitle(String title) {
		super.setTitle("MiEx | " + title);
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

}
