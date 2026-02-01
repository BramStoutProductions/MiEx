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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.ExportBounds.ExcludeRegion;
import nl.bramstout.mcworldexporter.MCWorldExporter;

public class WorldViewer2D extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, ToolTips.DynamicTooltip{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private CameraTransform transform;
	/**
	 * 0 : Make new selection 1 : Move whole selection 2 : Move left side 3 : Move
	 * right side 4 : Move top side 5 : Move bottom side 6 : Move top left corner 7
	 * : Move top right corner 8 : Move bottom left corner 9 : Move bottom right
	 * corner
	 */
	private int mouseGrabType;
	/**
	 * 0: Export bounds
	 * 1: LOD area
	 * 2: Export Chunk toggle
	 */
	private int mouseSelection;
	private int mouseButton;
	private int mouseDragStartX;
	private int mouseDragStartY;
	private CameraTransform transformStart;
	private ExportBounds selectionStart;
	private boolean snapToChunks;
	
	private int cursorX;
	private int cursorY;
	
	private Renderer2D renderer;
	private Thread rendererThread;
	private Timer animTimer;
	private BufferedImage selectionBuffer;
	
	public WorldViewer2D() {
		super();
		
		transform = new CameraTransform();
		mouseGrabType = 0;
		mouseSelection = 0;
		mouseButton = 0;
		mouseDragStartX = 0;
		mouseDragStartY = 0;
		transformStart = new CameraTransform();
		selectionStart = MCWorldExporter.getApp().getActiveExportBounds().copy();
		snapToChunks = false;
		
		renderer = new Renderer2D();
		rendererThread = new Thread(renderer);
		rendererThread.setName("Renderer2D");
		rendererThread.setDaemon(true);
		rendererThread.start();
		
		animTimer = new Timer(16, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
			
		});
		animTimer.start();
		

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		
		ToolTips.registerDynamicTooltip(this, this);
	}
	
	public void zoomIn() {
		transform.zoomLevel += 1;
		if (transform.zoomLevel > 8)
			transform.zoomLevel = 8;
		renderer.setCameraTransform(transform);
	}

	public void zoomOut() {
		transform.zoomLevel -= 1;
		if (transform.zoomLevel < 0)
			transform.zoomLevel = 0;
		renderer.setCameraTransform(transform);
	}

	public void reset() {
		transform.zoomLevel = 4;
		transform.camPosX = 0;
		transform.camPosY = 0;
		renderer.setCameraTransform(transform);
	}
	
	public void teleport(float x, float z) {
		transform.camPosX = x;
		transform.camPosY = z;
		renderer.setCameraTransform(transform);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);

		// Put in background colour
		g.setColor(new Color(32, 32, 32));
		g.fillRect(g.getClipBounds().x, g.getClipBounds().y, g.getClipBounds().width, g.getClipBounds().height);
		
		// If there is no world, skip
		if (MCWorldExporter.getApp().getWorld() == null)
			return;
		
		// Draw the frame buffer
		renderer.getFrameBufferLock().aqcuire();
		BufferedImage frameBuffer = renderer.getFrameBuffer();
		if(frameBuffer != null) {
			double zoom = Math.pow(2.0, transform.zoomLevel - renderer.getCameraTransform().zoomLevel);
			int bufferWidth = (int) (((double) frameBuffer.getWidth()) * zoom);
			int bufferHeight = (int) (((double) frameBuffer.getHeight()) * zoom);
			Point bufferOffset = transform.getPixelDifference(renderer.getCameraTransform(), 
					frameBuffer.getWidth(), frameBuffer.getHeight(), bufferWidth, bufferHeight);
			g.drawImage(frameBuffer, bufferOffset.ix(), bufferOffset.iy(), bufferWidth, bufferHeight, null);
		}
		renderer.getFrameBufferLock().release();
		
		// Give the renderer the current resolution, in case it changed, so that it can update it.
		AffineTransform gtransform = ((Graphics2D)g).getTransform();
		renderer.setResolution(getWidth(), getHeight());
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds("FG", g);
		
		int selectionBufferWidth = (int) (g.getClipBounds().width * gtransform.getScaleX());
		int selectionBufferHeight = (int) (g.getClipBounds().height * gtransform.getScaleY());
		if(selectionBuffer == null || (selectionBuffer.getWidth() != selectionBufferWidth || selectionBuffer.getHeight() != selectionBufferHeight)) {
			selectionBuffer = new BufferedImage(selectionBufferWidth, selectionBufferHeight, BufferedImage.TYPE_INT_ARGB);
		}
		
		RenderingHints renderingHints = ((Graphics2D)g).getRenderingHints();
		
		int[][] regionColors = new int[][] {
				new int[] {255, 0, 255},
				new int[] {255, 255, 0},
				new int[] {0, 255, 255},
				new int[] {255, 90, 90},
				new int[] {90, 90, 255},
				new int[] {90, 255, 90}
		};
		
		int regionI = 0;
		for(ExportBounds exportBounds : MCWorldExporter.getApp().getExportBoundsList()) {
			boolean isActive = MCWorldExporter.getApp().getActiveExportBounds().getName().equals(exportBounds.getName());
			float opacity = isActive ? 1f : 0.33f;
			
			boolean hasExcludeRegions = !exportBounds.excludeRegions.isEmpty();
			
			Graphics ebG = g;
			if(hasExcludeRegions) {
				ebG = selectionBuffer.createGraphics();
				((Graphics2D)ebG).setBackground(new Color(0, 0, 0, 0));
				((Graphics2D) ebG).addRenderingHints(renderingHints);
				((Graphics2D) ebG).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				((Graphics2D) ebG).setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				((Graphics2D) ebG).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
				ebG.setFont(g.getFont());
				((Graphics2D) ebG).setTransform(gtransform);
				ebG.clearRect(0, 0, getWidth(), getHeight());
			}
			
			Point selectionMinPos = transform.toScreen(new Point(exportBounds.getMinX(), exportBounds.getMinZ()), 
														getWidth(), getHeight());
			Point selectionMaxPos = transform.toScreen(new Point(exportBounds.getMaxX() + 1, exportBounds.getMaxZ() + 1), 
														getWidth(), getHeight());
			
			Point lodMinPos = transform.toScreen(new Point(exportBounds.getLodMinX(), exportBounds.getLodMinZ()), 
													getWidth(), getHeight());
			Point lodMaxPos = transform.toScreen(new Point(exportBounds.getLodMaxX() + 1, exportBounds.getLodMaxZ() + 1), 
													getWidth(), getHeight());
			
			
			int selectionWidth = selectionMaxPos.ix() - selectionMinPos.ix();
			int selectionHeight = selectionMaxPos.iy() - selectionMinPos.iy();
			int lodWidth = lodMaxPos.ix() - lodMinPos.ix();
			int lodHeight = lodMaxPos.iy() - lodMinPos.iy();
			if (selectionWidth > 1 && selectionHeight > 1) {
				int chunkStartX = exportBounds.getMinX() >> 4;
				int chunkStartZ = exportBounds.getMinZ() >> 4;
				int chunkEndX = exportBounds.getMaxX() >> 4;
				int chunkEndZ = exportBounds.getMaxZ() >> 4;
				
				ebG.setColor(new Color(255, 0, 255, (int) (48f * opacity)));
				//ebG.fillRect(selectionMinPos.ix(), selectionMinPos.iy(), selectionWidth, selectionHeight);
				
				int chunkI = 0;
				int chunkJ = 0;
				for(int j = chunkStartZ; j <= chunkEndZ; j += exportBounds.getChunkSize()) {
					chunkI = 0;
					int blockMinZ = j * 16;
					blockMinZ = Math.max(Math.min(blockMinZ, exportBounds.getMaxZ() + 1), exportBounds.getMinZ());
					int blockMaxZ = (j + exportBounds.getChunkSize()) * 16;
					blockMaxZ = Math.max(Math.min(blockMaxZ, exportBounds.getMaxZ() + 1), exportBounds.getMinZ());
					
					for(int i = chunkStartX; i <= chunkEndX; i += exportBounds.getChunkSize()) {
						int blockMinX = i * 16;
						blockMinX = Math.max(Math.min(blockMinX, exportBounds.getMaxX() + 1), exportBounds.getMinX());
						
						int blockMaxX = (i + exportBounds.getChunkSize()) * 16;
						blockMaxX = Math.max(Math.min(blockMaxX, exportBounds.getMaxX() + 1), exportBounds.getMinX());
						
						Point pixelMinPos = transform.toScreen(new Point(blockMinX, blockMinZ), getWidth(), getHeight());
						Point pixelMaxPos = transform.toScreen(new Point(blockMaxX, blockMaxZ), getWidth(), getHeight());
						
						if(exportBounds.isChunkEnabled(chunkI, chunkJ)) {
							ebG.fillRect(pixelMinPos.ix(), pixelMinPos.iy(), 
									pixelMaxPos.ix() - pixelMinPos.ix(), pixelMaxPos.iy() - pixelMinPos.iy());
						}
						
						chunkI++;
					}
					
					chunkJ++;
				}
				
				if(hasExcludeRegions) {
					// Draw exclude regions, which we do via clearing
					for(ExcludeRegion region : exportBounds.excludeRegions) {
						// Don't show any regions above or below out export bounds.
						if(region.maxY < exportBounds.minY || region.minY > exportBounds.maxY)
							continue;
						
						Point pixelMinPos = transform.toScreen(new Point(region.minX, region.minZ), getWidth(), getHeight());
						Point pixelMaxPos = transform.toScreen(new Point(region.maxX+1, region.maxZ+1), getWidth(), getHeight());
						
						if(region.maxY < exportBounds.maxY) {
							// The top part of the region is below the top part of the export bounds,
							// in order to help communicate this, we want to draw it at half opacity.
							// We do this by clearing every other line.
							// If we have overlapping regions, then we want the clear lines to overlap,
							// so let's make sure that minX is always a multiple of two.
							int minX = (pixelMinPos.ix() >> 1) << 1;
							if(minX < pixelMinPos.ix())
								minX += 2;
							for(int x = minX; x < pixelMaxPos.ix(); x += 2) {
								ebG.clearRect(x, pixelMinPos.iy(), 1, pixelMaxPos.iy() - pixelMinPos.iy());
							}
							int minY = (pixelMinPos.iy() >> 2) << 2;
							if(minY < pixelMinPos.iy())
								minY += 4;
							for(int y = minY; y < pixelMaxPos.iy(); y += 4) {
								ebG.clearRect(pixelMinPos.ix(), y, pixelMaxPos.ix() - pixelMinPos.ix(), 1);
							}
						}else {
							ebG.clearRect(pixelMinPos.ix(), pixelMinPos.iy(), 
									pixelMaxPos.ix() - pixelMinPos.ix(), pixelMaxPos.iy() - pixelMinPos.iy());
						}
					}
				}
				
				if(exportBounds.hasLod()) {
					ebG.setColor(new Color(0, 255, 255, (int) (48f * opacity)));
					ebG.fillRect(lodMinPos.ix(), lodMinPos.iy(), lodWidth, lodHeight);
				}
				
				ebG.setColor(new Color(128, 128, 128, (int) (128f * opacity)));
				
				// Draw vertical lines
				for(int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX += exportBounds.getChunkSize()) {
					int blockX = chunkX * 16;
					blockX = Math.max(Math.min(blockX, exportBounds.getMaxX()), exportBounds.getMinX());
					
					Point pixelPos = transform.toScreen(new Point(blockX, 0), getWidth(), getHeight());
					
					ebG.drawLine(pixelPos.ix(), selectionMinPos.iy(), pixelPos.ix(), selectionMaxPos.iy());
				}
				
				// Draw horizontal lines
				for(int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ += exportBounds.getChunkSize()) {
					int blockZ = chunkZ * 16;
					blockZ = Math.max(Math.min(blockZ, exportBounds.getMaxZ()), exportBounds.getMinZ());
					
					Point pixelPos = transform.toScreen(new Point(0, blockZ), getWidth(), getHeight());
					
					ebG.drawLine(selectionMinPos.ix(), pixelPos.iy(), selectionMaxPos.ix(), pixelPos.iy());
				}
				
				// Draw FG markers
				ebG.setColor(new Color(48, 255, 48, (int) (255f * opacity)));
				chunkI = 1;
				chunkJ = 1;
				for(int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ += exportBounds.getChunkSize()) {
					int blockMinZ = chunkZ * 16;
					blockMinZ = Math.max(Math.min(blockMinZ, exportBounds.getMaxZ()), exportBounds.getMinZ());
					
					int blockMaxZ = (chunkZ + exportBounds.getChunkSize()) * 16;
					blockMaxZ = Math.max(Math.min(blockMaxZ, exportBounds.getMaxZ()), exportBounds.getMinZ());
					
					for(int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX += exportBounds.getChunkSize()) {
						String chunkName = "chunk_" + chunkI + "_" + chunkJ;
						
						if(exportBounds.isChunkEnabled(chunkI - 1, chunkJ - 1)) {
							if((exportBounds.getFgChunks().isEmpty() && !MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks()) || 
									exportBounds.getFgChunks().contains(chunkName)) {
								int blockMinX = chunkX * 16;
								blockMinX = Math.max(Math.min(blockMinX, exportBounds.getMaxX()), exportBounds.getMinX());
								
								int blockMaxX = (chunkX + exportBounds.getChunkSize()) * 16;
								blockMaxX = Math.max(Math.min(blockMaxX, exportBounds.getMaxX()), exportBounds.getMinX());
								
								Point pixelMinPos = transform.toScreen(new Point(blockMinX, blockMinZ), getWidth(), getHeight());
								Point pixelMaxPos = transform.toScreen(new Point(blockMaxX, blockMaxZ), getWidth(), getHeight());
								
								int posX = (pixelMinPos.ix() + pixelMaxPos.ix()) / 2;
								int posY = (pixelMinPos.iy() + pixelMaxPos.iy()) / 2;
								
								
								ebG.drawString("FG", posX - ((int) (stringBounds.getWidth() / 2)), posY + ((int) (stringBounds.getHeight() / 2)));
							}
						}
						
						chunkI += 1;
					}
					chunkI = 1;
					chunkJ += 1;
				}
				
				
				
				ebG.setColor(new Color(
						regionColors[regionI%regionColors.length][0], 
						regionColors[regionI%regionColors.length][1], 
						regionColors[regionI%regionColors.length][2], 
						(int)(127f * opacity + 128f)));
				((Graphics2D)ebG).setStroke(new BasicStroke(isActive ? 2f : 0.5f));
				ebG.drawRect(selectionMinPos.ix(), selectionMinPos.iy(), selectionWidth, selectionHeight);
				
				if(hasExcludeRegions) {
					g.drawImage(selectionBuffer, 0, 0, getWidth(), getHeight(), null);
				}
			}
			
			regionI++;
		}
		
		Point cursorCoordinates = transform.toWorld(new Point(cursorX, cursorY), getWidth(), getHeight());
		renderer.setHeightSampleCoordinates(cursorCoordinates.ix(), cursorCoordinates.iy());
		String xCoord = Integer.toString(cursorCoordinates.ix());
		String yCoord = renderer.getHeightSample() == -1024 ? "" : Integer.toString(renderer.getHeightSample());
		String zCoord = Integer.toString(cursorCoordinates.iy());
		int ySpacing = ((int) stringBounds.getHeight())+3;
		int rightBounds = (int) g.getFontMetrics().getStringBounds("X: 0000000", g).getWidth() + 18;
		
		g.setColor(new Color(0, 0, 0, 192));
		g.fillRect(0, getHeight() - ySpacing * 3 - 16, rightBounds + 18, ySpacing * 3 + 16);
		
		g.setColor(new Color(128, 192, 255));
		g.drawString("X:", 18, getHeight() - ySpacing * 2 - 12);
		g.drawString("Y:", 18, getHeight() - ySpacing * 1 - 12);
		g.drawString("Z:", 18, getHeight() - ySpacing * 0 - 12);
		stringBounds = g.getFontMetrics().getStringBounds(xCoord, g);
		g.drawString(xCoord, rightBounds - ((int) stringBounds.getWidth()), getHeight() - ySpacing * 2 - 12);
		stringBounds = g.getFontMetrics().getStringBounds(yCoord, g);
		g.drawString(yCoord, rightBounds - ((int) stringBounds.getWidth()), getHeight() - ySpacing * 1 - 12);
		stringBounds = g.getFontMetrics().getStringBounds(zCoord, g);
		g.drawString(zCoord, rightBounds - ((int) stringBounds.getWidth()), getHeight() - ySpacing * 0 - 12);
		
		if(MCWorldExporter.getApp().getWorld().isPaused()) {
			g.setColor(new Color(32, 32, 32, 150));
			g.fillRect(g.getClipBounds().x, g.getClipBounds().y, g.getClipBounds().width, g.getClipBounds().height);
			
			g.setColor(new Color(180, 180, 180));
			stringBounds = g.getFontMetrics().getStringBounds("World Loading Paused", g);
			g.drawString("World Loading Paused", 
					((int) g.getClipBounds().getCenterX()) - ((int) (stringBounds.getWidth() / 2)), 
					((int) g.getClipBounds().getCenterY()) + ((int) (stringBounds.getHeight() / 2)));
		}
	}
	
	
	@Override
	public void mouseMoved(MouseEvent e) {
		cursorX = e.getX();
		cursorY = e.getY();
		if(MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks()) {
			this.setCursor(new Cursor(Cursor.HAND_CURSOR));
			mouseGrabType = 0;
			return;
		}
		mouseSelection = 0;
		if(e.isShiftDown())
			mouseSelection = 1;
		if(e.isControlDown())
			mouseSelection = 2;
		if(e.isAltDown())
			snapToChunks = true;
		else
			snapToChunks = false;
		int worldMinX = MCWorldExporter.getApp().getActiveExportBounds().getMinX();
		int worldMinZ = MCWorldExporter.getApp().getActiveExportBounds().getMinZ();
		int worldMaxX = MCWorldExporter.getApp().getActiveExportBounds().getMaxX();
		int worldMaxZ = MCWorldExporter.getApp().getActiveExportBounds().getMaxZ();
		if(mouseSelection == 1) {
			worldMinX = MCWorldExporter.getApp().getActiveExportBounds().getLodMinX();
			worldMinZ = MCWorldExporter.getApp().getActiveExportBounds().getLodMinZ();
			worldMaxX = MCWorldExporter.getApp().getActiveExportBounds().getLodMaxX();
			worldMaxZ = MCWorldExporter.getApp().getActiveExportBounds().getLodMaxZ();
		}
		Point selectionMinPos = transform.toScreen(new Point(worldMinX, worldMinZ), getWidth(), getHeight());
		Point selectionMaxPos = transform.toScreen(new Point(worldMaxX + 1, worldMaxZ + 1), getWidth(), getHeight());
		int selectionMinX = selectionMinPos.ix();
		int selectionMinZ = selectionMinPos.iy();
		int selectionMaxX = selectionMaxPos.ix();
		int selectionMaxZ = selectionMaxPos.iy();

		int mouseX = e.getX();
		int mouseY = e.getY();

		if (mouseX >= selectionMinX - 4 && mouseX <= selectionMaxX + 4 && mouseY >= selectionMinZ - 4
				&& mouseY <= selectionMaxZ + 4 && (mouseSelection != 1 || MCWorldExporter.getApp().getActiveExportBounds().hasLod())) {
			if (mouseY <= selectionMinZ + 4) {
				// Top Edge
				if (mouseX <= selectionMinX + 4) {
					// Top left corner
					this.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
					mouseGrabType = 6;
				} else if (mouseX >= selectionMaxX - 4) {
					// Top right corner
					this.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
					mouseGrabType = 7;
				} else {
					// Top Edge
					this.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
					mouseGrabType = 4;
				}
			} else if (mouseY >= selectionMaxZ - 4) {
				// Bottom Edge
				if (mouseX <= selectionMinX + 4) {
					// Top left corner
					this.setCursor(new Cursor(Cursor.SW_RESIZE_CURSOR));
					mouseGrabType = 8;
				} else if (mouseX >= selectionMaxX - 4) {
					// Top right corner
					this.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
					mouseGrabType = 9;
				} else {
					// Top Edge
					this.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
					mouseGrabType = 5;
				}
			} else if (mouseX <= selectionMinX + 4) {
				// Left Edge
				this.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
				mouseGrabType = 2;
			} else if (mouseX >= selectionMaxX - 4) {
				// Right Edge
				this.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
				mouseGrabType = 3;
			} else {
				// Center
				this.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				mouseGrabType = 1;
			}
		} else {
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			mouseGrabType = 0;
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		requestFocusInWindow();
		
		mouseSelection = e.isControlDown() ? 2 : (e.isShiftDown() ? 1 : 0);
		mouseDragStartX = e.getX();
		mouseDragStartY = e.getY();
		mouseButton = e.getButton();
		transformStart = transform.clone();
		selectionStart = MCWorldExporter.getApp().getActiveExportBounds().copy();
		if(e.isAltDown())
			snapToChunks = true;
		else
			snapToChunks = false;
		
		if(mouseSelection == 2) {
			if (mouseButton == MouseEvent.BUTTON1) {
				Point mouseBlock = transform.toWorld(new Point(e.getX(), e.getY()), getWidth(), getHeight());
				
				if(mouseBlock.ix() < MCWorldExporter.getApp().getActiveExportBounds().getMinX() || 
						mouseBlock.ix() > MCWorldExporter.getApp().getActiveExportBounds().getMaxX() ||
						mouseBlock.iy() < MCWorldExporter.getApp().getActiveExportBounds().getMinZ() ||
						mouseBlock.iy() > MCWorldExporter.getApp().getActiveExportBounds().getMaxZ())
					return;
				
				int chunkStartX = MCWorldExporter.getApp().getActiveExportBounds().getMinX() >> 4;
				int chunkStartZ = MCWorldExporter.getApp().getActiveExportBounds().getMinZ() >> 4;
				
				int mouseChunkX = mouseBlock.ix() >> 4;
				int mouseChunkZ = mouseBlock.iy() >> 4;
				
				int chunkX = mouseChunkX - chunkStartX;
				int chunkZ = mouseChunkZ - chunkStartZ;
				
				if(chunkX < 0 || chunkZ < 0)
					return;
				
				chunkX /= MCWorldExporter.getApp().getActiveExportBounds().getChunkSize();
				chunkZ /= MCWorldExporter.getApp().getActiveExportBounds().getChunkSize();
				
				MCWorldExporter.getApp().getActiveExportBounds().toggleChunk(chunkX, chunkZ);
			}
		}else {
			if(MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks()) {
				if (mouseButton == MouseEvent.BUTTON1) {
					Point mouseBlock = transform.toWorld(new Point(e.getX(), e.getY()), getWidth(), getHeight());
					
					if(mouseBlock.ix() < MCWorldExporter.getApp().getActiveExportBounds().getMinX() || 
							mouseBlock.ix() > MCWorldExporter.getApp().getActiveExportBounds().getMaxX() ||
							mouseBlock.iy() < MCWorldExporter.getApp().getActiveExportBounds().getMinZ() ||
							mouseBlock.iy() > MCWorldExporter.getApp().getActiveExportBounds().getMaxZ())
						return;
					
					int chunkStartX = MCWorldExporter.getApp().getActiveExportBounds().getMinX() >> 4;
					int chunkStartZ = MCWorldExporter.getApp().getActiveExportBounds().getMinZ() >> 4;
					
					int mouseChunkX = mouseBlock.ix() >> 4;
					int mouseChunkZ = mouseBlock.iy() >> 4;
					
					int chunkX = mouseChunkX - chunkStartX;
					int chunkZ = mouseChunkZ - chunkStartZ;
					
					if(chunkX < 0 || chunkZ < 0)
						return;
					
					chunkX /= MCWorldExporter.getApp().getActiveExportBounds().getChunkSize();
					chunkZ /= MCWorldExporter.getApp().getActiveExportBounds().getChunkSize();
					
					chunkX += 1;
					chunkZ += 1;
					
					String chunkName = "chunk_" + chunkX + "_" + chunkZ;
					
					if(MCWorldExporter.getApp().getActiveExportBounds().getFgChunks().contains(chunkName))
						MCWorldExporter.getApp().getActiveExportBounds().getFgChunks().remove(chunkName);
					else
						MCWorldExporter.getApp().getActiveExportBounds().getFgChunks().add(chunkName);
				}
			}
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		cursorX = e.getX();
		cursorY = e.getY();
		if(MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks())
			return;
		
		Point mouseBlockStart = transform.toWorld(new Point(mouseDragStartX, mouseDragStartY), getWidth(), getHeight());
		Point mouseBlockCurrent = transform.toWorld(new Point(e.getX(), e.getY()), getWidth(), getHeight());
		Point mouseBlockDelta = new Point(mouseBlockCurrent.x - mouseBlockStart.x, mouseBlockCurrent.y - mouseBlockStart.y);
		
		if(e.isAltDown())
			snapToChunks = true;
		else
			snapToChunks = false;
		
		if (mouseButton == MouseEvent.BUTTON1) {
			int deltaX = mouseBlockDelta.ix();
			int deltaZ = mouseBlockDelta.iy();
			
			if(mouseGrabType == 0) {
				// New selection
				
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							mouseBlockStart.ix(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							mouseBlockStart.iy(), 
							mouseBlockCurrent.ix(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							mouseBlockCurrent.iy());
					if(snapToChunks)
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunks();
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							mouseBlockStart.ix(), 
							mouseBlockStart.iy(), 
							mouseBlockCurrent.ix(), 
							mouseBlockCurrent.iy());
				}
			} else if(mouseGrabType == 1) {
				// Move whole selection
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ() + deltaZ, 
							selectionStart.getMaxX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ() + deltaZ);
					if(snapToChunks)
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunks();
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX() + deltaX,  
							selectionStart.getLodMinZ() + deltaZ, 
							selectionStart.getLodMaxX() + deltaX,  
							selectionStart.getLodMaxZ() + deltaZ);
				}
			} else if(mouseGrabType == 2) {
				// Move left side
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ(), 
							selectionStart.getMaxX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ());
					if(snapToChunks)
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMinX();
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX() + deltaX,  
							selectionStart.getLodMinZ(), 
							selectionStart.getLodMaxX(),  
							selectionStart.getLodMaxZ());
				}
			} else if(mouseGrabType == 3) {
				// Move right side
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ(), 
							selectionStart.getMaxX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ());
					if(snapToChunks)
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMaxX();
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX(),  
							selectionStart.getLodMinZ(), 
							selectionStart.getLodMaxX() + deltaX,  
							selectionStart.getLodMaxZ());
				}
			} else if(mouseGrabType == 4) {
				// Move top side
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ() + deltaZ, 
							selectionStart.getMaxX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ());
					if(snapToChunks)
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMinZ();
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX(),  
							selectionStart.getLodMinZ() + deltaZ, 
							selectionStart.getLodMaxX(),  
							selectionStart.getLodMaxZ());
				}
			} else if(mouseGrabType == 5) {
				// Move bottom side
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ(), 
							selectionStart.getMaxX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ() + deltaZ);
					if(snapToChunks)
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMaxZ();
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX(), 
							selectionStart.getLodMinZ(), 
							selectionStart.getLodMaxX(), 
							selectionStart.getLodMaxZ() + deltaZ);
				}
			} else if(mouseGrabType == 6) {
				// Move top left corner
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ() + deltaZ, 
							selectionStart.getMaxX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ());
					if(snapToChunks) {
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMinX();
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMinZ();
					}
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX() + deltaX, 
							selectionStart.getLodMinZ() + deltaZ, 
							selectionStart.getLodMaxX(),  
							selectionStart.getLodMaxZ());
				}
			} else if(mouseGrabType == 7) {
				// Move top right corner
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ() + deltaZ, 
							selectionStart.getMaxX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ());
					if(snapToChunks) {
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMaxX();
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMinZ();
					}
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX(),  
							selectionStart.getLodMinZ() + deltaZ, 
							selectionStart.getLodMaxX() + deltaX,  
							selectionStart.getLodMaxZ());
				}
			} else if(mouseGrabType == 8) {
				// Move bottom left corner
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ(), 
							selectionStart.getMaxX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ() + deltaZ);
					if(snapToChunks) {
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMinX();
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMaxZ();
					}
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX() + deltaX,  
							selectionStart.getLodMinZ(), 
							selectionStart.getLodMaxX(),  
							selectionStart.getLodMaxZ() + deltaZ);
				}
			} else if(mouseGrabType == 9) {
				// Move bottom right corner
				if(mouseSelection == 0) {
					MCWorldExporter.getApp().getActiveExportBounds().set(
							selectionStart.getMinX(), 
							MCWorldExporter.getApp().getActiveExportBounds().getMinY(), 
							selectionStart.getMinZ(), 
							selectionStart.getMaxX() + deltaX, 
							MCWorldExporter.getApp().getActiveExportBounds().getMaxY(), 
							selectionStart.getMaxZ() + deltaZ);
					if(snapToChunks) {
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMaxX();
						MCWorldExporter.getApp().getActiveExportBounds().snapToChunksMaxZ();
					}
				}else if(mouseSelection == 1) {
					MCWorldExporter.getApp().getActiveExportBounds().setLod(
							selectionStart.getLodMinX(),  
							selectionStart.getLodMinZ(), 
							selectionStart.getLodMaxX() + deltaX,  
							selectionStart.getLodMaxZ() + deltaZ);
				}
			}
		} else if (mouseButton == MouseEvent.BUTTON3) {
			teleport(transformStart.camPosX - mouseBlockDelta.x, transformStart.camPosY - mouseBlockDelta.y);
		}
	}
	
	
	
	
	
	
	
	
	public static class Point{
		public float x;
		public float y;
		
		public Point() {
			this.x = 0;
			this.y = 0;
		}
		
		public Point(float x, float y) {
			this.x = x;
			this.y = y;
		}
		
		public int ix() {
			return (int) Math.floor(x);
		}
		
		public int iy() {
			return (int) Math.floor(y);
		}
	}
	
	public static class CameraTransform{
		
		/**
		 * How much zoom there is. 0 means each chunk is a single pixel. 1 means each
		 * chunk is 2x2 pixels. 2 means each chunk is 4x4 pixels. 3 means each chunk is
		 * 8x8 pixels. 4 means each chunk is 16x16 pixels. And so on.
		 */
		public int zoomLevel=4;
		public float camPosX=0f;
		public float camPosY=0f;
		
		public Point toWorld(Point screen, int width, int height) {
			float x = screen.x - (((float) width) / 2f);
			float y = screen.y - (((float) height) / 2f);
			
			x *= Math.pow(2.0f, 4-zoomLevel);
			y *= Math.pow(2.0f, 4-zoomLevel);
			
			x += camPosX;
			y += camPosY;
			
			return new Point(x, y);
		}
		
		public Point toScreen(Point world, int width, int height) {
			float x = world.x - camPosX;
			float y = world.y - camPosY;
			
			x /= Math.pow(2.0f,  4-zoomLevel);
			y /= Math.pow(2.0f, 4-zoomLevel);
			
			x += (((float) width) / 2f);
			y += (((float) height) / 2f);
			
			return new Point(x, y);
		}
		
		public Point getPixelDifference(CameraTransform other, int width, int height, int otherWidth, int otherHeight) {
			Point a = toScreen(new Point(camPosX, camPosY), width, height);
			Point b = other.toScreen(new Point(camPosX, camPosY), otherWidth, otherHeight);
			return new Point(a.x - b.x, a.y - b.y);
		}
		
		public CameraTransform clone() {
			CameraTransform copy = new CameraTransform();
			copy.zoomLevel = zoomLevel;
			copy.camPosX = camPosX;
			copy.camPosY = camPosY;
			return copy;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof CameraTransform))
				return false;
			CameraTransform other = (CameraTransform) obj;
			return other.zoomLevel == zoomLevel && other.camPosX == camPosX && other.camPosY == camPosY;
		}
		
	}
	
	
	
	

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	public void fullReRender() {
		if(MCWorldExporter.getApp().getWorld() != null)
			MCWorldExporter.getApp().getWorld().forceReRender();
		renderer.requestRender();
	}
	
	private long lastWheelRotation = 0;

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		e.consume();
		// Prevent really fast zooming and accidental
		// double zooms.
		if((e.getWhen() - lastWheelRotation) < 500)
			return;
		if(e.getWheelRotation() < 0)
			zoomIn();
		if(e.getWheelRotation() > 0)
			zoomOut();
	}

	@Override
	public String getTooltip() {
		if(MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks()) {
			return "Click on an export chunk to toggle whether it should be tagged as a foreground chunk or not. Foreground chunks have \"FG\" in it.";
		}
		if(mouseSelection == 1) {
			return "Edit the LOD area selection. The blue area is the full quality area. Drag outside the current LOD area to make a new one. Drag the centre to move it. Drag the edges to expand or contract it.";
		}
		if(mouseSelection == 2) {
			return "Click on export chunks to enable or disable them. Disabled chunks won't show a magenta overlay.";
		}
		return "Move around the world or make an export selection. Drag outside the current selection to make a new one. Drag the centre to move it. Drag the edges to expand or contract it. Hold control to enable or disable specific chunks. Hold alt down in order to snap the region to chunks";
	}

}
