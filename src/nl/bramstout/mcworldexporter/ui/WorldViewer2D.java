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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.ExportBounds;
import nl.bramstout.mcworldexporter.MCWorldExporter;

public class WorldViewer2D extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener{

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
	private int mouseButton;
	private int mouseDragStartX;
	private int mouseDragStartY;
	private CameraTransform transformStart;
	private ExportBounds selectionStart;
	
	private Renderer2D renderer;
	private Thread rendererThread;
	private Timer animTimer;
	
	public WorldViewer2D() {
		super();
		
		transform = new CameraTransform();
		mouseGrabType = 0;
		mouseButton = 0;
		mouseDragStartX = 0;
		mouseDragStartY = 0;
		transformStart = new CameraTransform();
		selectionStart = MCWorldExporter.getApp().getExportBounds().copy();
		
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
		renderer.setResolution(getWidth(), getHeight());
		
		Point selectionMinPos = transform.toScreen(new Point(MCWorldExporter.getApp().getExportBounds().getMinX(), 
															MCWorldExporter.getApp().getExportBounds().getMinZ()), getWidth(), getHeight());
		Point selectionMaxPos = transform.toScreen(new Point(MCWorldExporter.getApp().getExportBounds().getMaxX() + 1, 
															MCWorldExporter.getApp().getExportBounds().getMaxZ() + 1), getWidth(), getHeight());
		
		Point lodMinPos = transform.toScreen(new Point(MCWorldExporter.getApp().getExportBounds().getLodMinX(), 
				MCWorldExporter.getApp().getExportBounds().getLodMinZ()), getWidth(), getHeight());
		Point lodMaxPos = transform.toScreen(new Point(MCWorldExporter.getApp().getExportBounds().getLodMaxX() + 1, 
						MCWorldExporter.getApp().getExportBounds().getLodMaxZ() + 1), getWidth(), getHeight());
		
		int selectionWidth = selectionMaxPos.ix() - selectionMinPos.ix();
		int selectionHeight = selectionMaxPos.iy() - selectionMinPos.iy();
		int lodWidth = lodMaxPos.ix() - lodMinPos.ix();
		int lodHeight = lodMaxPos.iy() - lodMinPos.iy();
		if (selectionWidth > 1 && selectionHeight > 1) {
			g.setColor(new Color(255, 0, 255, 48));
			g.fillRect(selectionMinPos.ix(), selectionMinPos.iy(), selectionWidth, selectionHeight);
			
			if(MCWorldExporter.getApp().getExportBounds().hasLod()) {
				g.setColor(new Color(0, 255, 255, 48));
				g.fillRect(lodMinPos.ix(), lodMinPos.iy(), lodWidth, lodHeight);
			}
			
			g.setColor(new Color(128, 128, 128, 128));
			
			int chunkStartX = MCWorldExporter.getApp().getExportBounds().getMinX() >> 4;
			int chunkStartZ = MCWorldExporter.getApp().getExportBounds().getMinZ() >> 4;
			int chunkEndX = MCWorldExporter.getApp().getExportBounds().getMaxX() >> 4;
			int chunkEndZ = MCWorldExporter.getApp().getExportBounds().getMaxZ() >> 4;
			
			// Draw vertical lines
			for(int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX += Config.chunkSize) {
				int blockX = chunkX * 16;
				blockX = Math.max(Math.min(blockX, MCWorldExporter.getApp().getExportBounds().getMaxX()), MCWorldExporter.getApp().getExportBounds().getMinX());
				
				Point pixelPos = transform.toScreen(new Point(blockX, 0), getWidth(), getHeight());
				
				g.drawLine(pixelPos.ix(), selectionMinPos.iy(), pixelPos.ix(), selectionMaxPos.iy());
			}
			
			// Draw horizontal lines
			for(int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ += Config.chunkSize) {
				int blockZ = chunkZ * 16;
				blockZ = Math.max(Math.min(blockZ, MCWorldExporter.getApp().getExportBounds().getMaxZ()), MCWorldExporter.getApp().getExportBounds().getMinZ());
				
				Point pixelPos = transform.toScreen(new Point(0, blockZ), getWidth(), getHeight());
				
				g.drawLine(selectionMinPos.ix(), pixelPos.iy(), selectionMaxPos.ix(), pixelPos.iy());
			}
			
			// Draw FG markers
			g.setColor(new Color(48, 255, 48));
			int chunkI = 1;
			int chunkJ = 1;
			for(int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ += Config.chunkSize) {
				int blockMinZ = chunkZ * 16;
				blockMinZ = Math.max(Math.min(blockMinZ, MCWorldExporter.getApp().getExportBounds().getMaxZ()), MCWorldExporter.getApp().getExportBounds().getMinZ());
				
				int blockMaxZ = (chunkZ + Config.chunkSize) * 16;
				blockMaxZ = Math.max(Math.min(blockMaxZ, MCWorldExporter.getApp().getExportBounds().getMaxZ()), MCWorldExporter.getApp().getExportBounds().getMinZ());
				
				for(int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX += Config.chunkSize) {
					String chunkName = "chunk_" + chunkI + "_" + chunkJ;
					
					if((MCWorldExporter.getApp().getFGChunks().isEmpty() && !MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks()) || 
							MCWorldExporter.getApp().getFGChunks().contains(chunkName)) {
						int blockMinX = chunkX * 16;
						blockMinX = Math.max(Math.min(blockMinX, MCWorldExporter.getApp().getExportBounds().getMaxX()), 
											MCWorldExporter.getApp().getExportBounds().getMinX());
						
						int blockMaxX = (chunkX + Config.chunkSize) * 16;
						blockMaxX = Math.max(Math.min(blockMaxX, MCWorldExporter.getApp().getExportBounds().getMaxX()), 
											MCWorldExporter.getApp().getExportBounds().getMinX());
						
						Point pixelMinPos = transform.toScreen(new Point(blockMinX, blockMinZ), getWidth(), getHeight());
						Point pixelMaxPos = transform.toScreen(new Point(blockMaxX, blockMaxZ), getWidth(), getHeight());
						
						int posX = (pixelMinPos.ix() + pixelMaxPos.ix()) / 2;
						int posY = (pixelMinPos.iy() + pixelMaxPos.iy()) / 2;
						
						Rectangle2D stringBounds = g.getFontMetrics().getStringBounds("FG", g);
						g.drawString("FG", posX - ((int) (stringBounds.getWidth() / 2)), posY + ((int) (stringBounds.getHeight() / 2)));
					}
					
					chunkI += 1;
				}
				chunkI = 1;
				chunkJ += 1;
			}
			
			
			
			g.setColor(new Color(255, 0, 255));
			g.drawRect(selectionMinPos.ix(), selectionMinPos.iy(), selectionWidth, selectionHeight);
		}
	}
	
	
	@Override
	public void mouseMoved(MouseEvent e) {
		if(MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks()) {
			this.setCursor(new Cursor(Cursor.HAND_CURSOR));
			mouseGrabType = 0;
			return;
		}
		Point selectionMinPos = transform.toScreen(new Point(MCWorldExporter.getApp().getExportBounds().getMinX(), 
				MCWorldExporter.getApp().getExportBounds().getMinZ()), getWidth(), getHeight());
		Point selectionMaxPos = transform.toScreen(new Point(MCWorldExporter.getApp().getExportBounds().getMaxX() + 1, 
						MCWorldExporter.getApp().getExportBounds().getMaxZ() + 1), getWidth(), getHeight());
		int selectionMinX = selectionMinPos.ix();
		int selectionMinZ = selectionMinPos.iy();
		int selectionMaxX = selectionMaxPos.ix();
		int selectionMaxZ = selectionMaxPos.iy();

		int mouseX = e.getX();
		int mouseY = e.getY();

		if (mouseX >= selectionMinX - 4 && mouseX <= selectionMaxX + 4 && mouseY >= selectionMinZ - 4
				&& mouseY <= selectionMaxZ + 4) {
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
		mouseDragStartX = e.getX();
		mouseDragStartY = e.getY();
		mouseButton = e.getButton();
		transformStart = transform.clone();
		selectionStart = MCWorldExporter.getApp().getExportBounds().copy();
		
		if(MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks()) {
			if (mouseButton == MouseEvent.BUTTON1) {
				Point mouseBlock = transform.toWorld(new Point(e.getX(), e.getY()), getWidth(), getHeight());
				
				if(mouseBlock.ix() < MCWorldExporter.getApp().getExportBounds().getMinX() || 
						mouseBlock.ix() > MCWorldExporter.getApp().getExportBounds().getMaxX() ||
						mouseBlock.iy() < MCWorldExporter.getApp().getExportBounds().getMinZ() ||
						mouseBlock.iy() > MCWorldExporter.getApp().getExportBounds().getMaxZ())
					return;
				
				int chunkStartX = MCWorldExporter.getApp().getExportBounds().getMinX() >> 4;
				int chunkStartZ = MCWorldExporter.getApp().getExportBounds().getMinZ() >> 4;
				
				int mouseChunkX = mouseBlock.ix() >> 4;
				int mouseChunkZ = mouseBlock.iy() >> 4;
				
				int chunkX = mouseChunkX - chunkStartX;
				int chunkZ = mouseChunkZ - chunkStartZ;
				
				if(chunkX < 0 || chunkZ < 0)
					return;
				
				chunkX /= Config.chunkSize;
				chunkZ /= Config.chunkSize;
				
				chunkX += 1;
				chunkZ += 1;
				
				String chunkName = "chunk_" + chunkX + "_" + chunkZ;
				
				if(MCWorldExporter.getApp().getFGChunks().contains(chunkName))
					MCWorldExporter.getApp().getFGChunks().remove(chunkName);
				else
					MCWorldExporter.getApp().getFGChunks().add(chunkName);
			}
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(MCWorldExporter.getApp().getUI().getToolbar().isEditingFGChunks())
			return;
		
		Point mouseBlockStart = transform.toWorld(new Point(mouseDragStartX, mouseDragStartY), getWidth(), getHeight());
		Point mouseBlockCurrent = transform.toWorld(new Point(e.getX(), e.getY()), getWidth(), getHeight());
		Point mouseBlockDelta = new Point(mouseBlockCurrent.x - mouseBlockStart.x, mouseBlockCurrent.y - mouseBlockStart.y);
		
		if (mouseButton == MouseEvent.BUTTON1) {
			int deltaX = mouseBlockDelta.ix();
			int deltaZ = mouseBlockDelta.iy();
			
			if(mouseGrabType == 0) {
				// New selection
				MCWorldExporter.getApp().getExportBounds().set(mouseBlockStart.ix(), MCWorldExporter.getApp().getExportBounds().getMinY(), 
						mouseBlockStart.iy(), mouseBlockCurrent.ix(), MCWorldExporter.getApp().getExportBounds().getMaxY(), mouseBlockCurrent.iy());
			} else if(mouseGrabType == 1) {
				// Move whole selection
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ() + deltaZ, 
						selectionStart.getMaxX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ() + deltaZ);
			} else if(mouseGrabType == 2) {
				// Move left side
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ(), 
						selectionStart.getMaxX(), 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ());
			} else if(mouseGrabType == 3) {
				// Move right side
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX(), 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ(), 
						selectionStart.getMaxX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ());
			} else if(mouseGrabType == 4) {
				// Move top side
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX(), 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ() + deltaZ, 
						selectionStart.getMaxX(), 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ());
			} else if(mouseGrabType == 5) {
				// Move bottom side
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX(), 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ(), 
						selectionStart.getMaxX(), 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ() + deltaZ);
			} else if(mouseGrabType == 6) {
				// Move top left corner
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ() + deltaZ, 
						selectionStart.getMaxX(), 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ());
			} else if(mouseGrabType == 7) {
				// Move top right corner
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX(), 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ() + deltaZ, 
						selectionStart.getMaxX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ());
			} else if(mouseGrabType == 8) {
				// Move bottom left corner
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ(), 
						selectionStart.getMaxX(), 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ() + deltaZ);
			} else if(mouseGrabType == 9) {
				// Move bottom right corner
				MCWorldExporter.getApp().getExportBounds().set(
						selectionStart.getMinX(), 
						MCWorldExporter.getApp().getExportBounds().getMinY(), 
						selectionStart.getMinZ(), 
						selectionStart.getMaxX() + deltaX, 
						MCWorldExporter.getApp().getExportBounds().getMaxY(), 
						selectionStart.getMaxZ() + deltaZ);
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

}
