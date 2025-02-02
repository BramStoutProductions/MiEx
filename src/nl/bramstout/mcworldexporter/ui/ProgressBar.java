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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Window.Type;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

public class ProgressBar extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private float progress;
	private float prevProgress;
	private int numChunks;
	private String text;
	private String tooltip;
	private long lastTooltipUpdate = 0;
	private int lastMouseX = 0;
	private int lastMouseY = 0;
	
	public ProgressBar() {
		super();
		progress = 0f;
		prevProgress = 0f;
		numChunks = 1;
		text = "";
		tooltip = "";
		setMinimumSize(new Dimension(10, 15));
		setMaximumSize(new Dimension(10000, 15));
		setPreferredSize(new Dimension(500, 15));
		
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

			@Override
			public void eventDispatched(AWTEvent event) {
				long currentTime = System.currentTimeMillis();
				if(event instanceof MouseEvent) {
					if(currentTime - lastTooltipUpdate < 100)
						return;
					MouseEvent e = (MouseEvent) event;
					lastMouseX = e.getXOnScreen();
					lastMouseY = e.getYOnScreen();
				}
				
				Window currentWindow = null;
				for(Window window : Window.getWindows()) {
					if(window.isActive() && window.isShowing()) {
						if(currentWindow == null)
							currentWindow = window;
					}
					if(window.isShowing() && window.getType() == Type.POPUP)
						currentWindow = window;
				}
				if(currentWindow != null) {
					int x = lastMouseX - currentWindow.getLocationOnScreen().x;
					int y = lastMouseY - currentWindow.getLocationOnScreen().y;
					Component comp = currentWindow.findComponentAt(x, y);
					if(comp != null) {
						tooltip = ToolTips.getTooltip(comp);
						lastTooltipUpdate = currentTime;
						paintImmediately(0, 0, getWidth(), getHeight());
					}
				}
			}
			
		}, AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(new Color(64, 96, 255));
		g.fillRect(0, 0, (int) (progress * getWidth()), getHeight());
		g.setFont(getFont().deriveFont(10));
		g.setColor(Color.BLACK);
		g.drawString(text.isEmpty() ? tooltip : text, 7, 11);
	}
	
	public void setProgress(float progress) {
		this.progress = progress;
		progressChanged();
	}
	
	public void setNumChunks(int numChunks) {
		this.numChunks = numChunks;
	}
	
	private void progressChanged() {
		if(Math.abs(progress - prevProgress) > 0.001f) {
			prevProgress = progress;
			paintImmediately(0, 0, getWidth(), getHeight());
		}
	}
	
	public void finishedMesh(int numMeshes) {
		progress += 1.0f / ((float) numMeshes) / ((float) numChunks) * 0.75f;
		progressChanged();
	}
	
	public void finishedOptimising(int numMeshes) {
		progress += 1.0f / ((float) numMeshes) / ((float) numChunks) * 0.25f;
		progressChanged();
	}
	
	public void finishedChunk() {
		progress += 1.0f / ((float) numChunks);
		progressChanged();
	}
	
	public void setText(String text) {
		this.text = text;
		paintImmediately(0, 0, getWidth(), getHeight());
	}

}
