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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.parallel.BackgroundThread;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.Region;
import nl.bramstout.mcworldexporter.world.World;
import nl.bramstout.mcworldexporter.world.anvil.ChunkAnvil;
import nl.bramstout.mcworldexporter.world.bedrock.ChunkBedrock;

public class RegionViewer extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private float regionSize;
	private float xPos;
	private float yPos;
	private float xOffset;
	private float yOffset;
	
	private int cursorX;
	private int cursorY;
	
	private BufferedImage buffer;
	private World prevWorld;
	private String prevDimension;
	
	public RegionViewer() {
		buffer = null;
		prevWorld = null;
		prevDimension = null;
		cursorX = 0;
		cursorY = 0;
		regionSize = 1f;
		
		setCursor(Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB), 
				new Point(), "emptyCursor"));
		
		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				cursorX = e.getX();
				cursorY = e.getY();
				repaint();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				cursorX = e.getX();
				cursorY = e.getY();
				repaint();
			}
			
		});
	}
	
	public Point getCursorWorldLocation() {
		float xWorld = ((cursorX - xPos) * 32f * 16f) / regionSize + (xOffset * 32f * 16f);
		float zWorld = ((cursorY - yPos) * 32f * 16f) / regionSize + (yOffset * 32f * 16f);
		
		return new Point((int) xWorld, (int) zWorld);
	}
	
	private void paintBuffer() {
		BackgroundThread.runInBackground(()->{
			Graphics2D g2 = (Graphics2D) buffer.getGraphics();
			g2.setBackground(Color.BLACK);
			g2.clearRect(0, 0, getWidth(), getHeight());
			
			g2.setColor(new Color(127, 127, 127));
			
			World world = MCWorldExporter.getApp().getWorld();
			if(world == null)
				return;
			
			int regionMinX = world.getRegionMinX();
			int regionMinZ = world.getRegionMinZ();
			int regionMaxX = world.getRegionMaxX();
			int regionMaxZ = world.getRegionMaxZ();
			int padding = Math.max(regionMaxX - regionMinX, regionMaxZ - regionMinZ) / 16;
			regionMinX -= padding;
			regionMinZ -= padding;
			regionMaxX += padding;
			regionMaxZ += padding;
			float regionWidth = regionMaxX - regionMinX + 1;
			float regionHeight = regionMaxZ - regionMinZ + 1;
			
			regionSize = Math.max(Math.min(((float) getWidth()) / regionWidth, ((float) getHeight()) / regionHeight),0.000000001f);
			float imgWidth = regionWidth * regionSize;
			float imgHeight = regionHeight * regionSize;
			
			xPos = (((float) getWidth()) - imgWidth) / 2f;
			yPos = (((float) getHeight()) - imgHeight) / 2f;
			xOffset = regionMinX;
			yOffset = regionMinZ;
			
			if(world.getRegions() != null) {
				for(Region region : world.getRegions()) {
					if(region == null)
						continue;
					int regionX = region.getXCoordinate();
					int regionZ = region.getZCoordinate();
					
					for(int chunkZ = 0; chunkZ < region.getStride(); ++chunkZ) {
						for(int chunkX = 0; chunkX < region.getStride(); ++chunkX) {
							try {
								Chunk chunk = region.getChunk((regionX * region.getStride()) + chunkX, (regionZ * region.getStride()) + chunkZ);
								if(chunk == null)
									continue;
								
								if(chunk instanceof ChunkAnvil) {
									if(((ChunkAnvil) chunk).getDataSize() <= 0)
										continue;
								}else if(chunk instanceof ChunkBedrock) {
									continue;
								}
								
								float xStart = (regionX-regionMinX) * regionSize + (chunkX * regionSize) / ((float) region.getStride()) + xPos;
								float yStart = (regionZ-regionMinZ) * regionSize + (chunkZ * regionSize) / ((float) region.getStride()) + yPos;
								float xEnd = (regionX-regionMinX) * regionSize + ((chunkX+1) * regionSize) / ((float) region.getStride()) + xPos;
								float yEnd = (regionZ-regionMinZ) * regionSize + ((chunkZ+1) * regionSize) / ((float) region.getStride()) + yPos;
								g2.fillRect((int) xStart, (int) yStart, (int) Math.max(xEnd-xStart,1), (int) Math.max(yEnd-yStart,1));
							}catch(Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
			
			SwingUtilities.invokeLater(()->{
				repaint();
			});
		});
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		boolean redraw = false;
		if(MCWorldExporter.getApp().getWorld() != prevWorld)
			redraw = true;
		prevWorld = MCWorldExporter.getApp().getWorld();
		if(MCWorldExporter.getApp().getWorld() != null) {
			if(MCWorldExporter.getApp().getWorld().getCurrentDimensions() != prevDimension)
				redraw = true;
			prevDimension = MCWorldExporter.getApp().getWorld().getCurrentDimensions();
		}
		
		if(buffer == null || (buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight())) {
			redraw = true;
			buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		}
		
		if(redraw)
			paintBuffer();
		
		g.drawImage(buffer, 0, 0, null);
		
		g.setColor(Color.white);
		
		g.drawLine(cursorX - 5, cursorY, cursorX + 5, cursorY);
		g.drawLine(cursorX, cursorY - 5, cursorX, cursorY + 5);
		
		Point cursorWorldPos = getCursorWorldLocation();
		g.drawString(Integer.toString(cursorWorldPos.x) + "," + Integer.toString(cursorWorldPos.y), cursorX + 4, cursorY - 4);
	}

}
