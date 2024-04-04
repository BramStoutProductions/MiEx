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
import java.awt.Graphics;

import javax.swing.JPanel;

public class ProgressBar extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private float progress;
	private float prevProgress;
	private int numChunks;
	
	public ProgressBar() {
		super();
		progress = 0f;
		prevProgress = 0f;
		numChunks = 1;
		setMinimumSize(new Dimension(10, 5));
		setMaximumSize(new Dimension(10000, 5));
		setPreferredSize(new Dimension(500, 5));
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.BLUE);
		g.fillRect(0, 0, (int) (progress * getWidth()), getHeight());
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

}
