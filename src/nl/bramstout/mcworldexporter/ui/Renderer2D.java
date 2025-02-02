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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.parallel.SpinLock;
import nl.bramstout.mcworldexporter.parallel.ThreadPool;
import nl.bramstout.mcworldexporter.ui.WorldViewer2D.CameraTransform;
import nl.bramstout.mcworldexporter.ui.WorldViewer2D.Point;
import nl.bramstout.mcworldexporter.world.Chunk;
import nl.bramstout.mcworldexporter.world.World;

public class Renderer2D implements Runnable {

	private static ThreadPool threadPool = new ThreadPool(1536);

	private BufferedImage buffer;
	private BufferedImage heightBuffer;
	private BufferedImage frontBuffer;
	private BufferedImage postBuffer;
	private BufferedImage tmpBuffer;
	private int bufferWidth;
	private int bufferHeight;
	private CameraTransform bufferTransform;
	private CameraTransform frontBufferTransform;
	private SpinLock frontBufferLock;

	private Vector<Chunk> finishedChunksStack;

	private AtomicBoolean renderRequested;
	private AtomicBoolean clearBuffer;

	private int minChunkX;
	private int minChunkZ;
	private int maxChunkX;
	private int maxChunkZ;
	
	private int renderCounter;

	public Renderer2D() {
		buffer = null;
		heightBuffer = null;
		frontBuffer = null;
		postBuffer = null;
		tmpBuffer = null;

		renderRequested = new AtomicBoolean(false);
		clearBuffer = new AtomicBoolean(false);
		bufferTransform = new CameraTransform();
		frontBufferTransform = new CameraTransform();
		frontBufferLock = new SpinLock();

		finishedChunksStack = new Vector<Chunk>(1024, 1024);
		
		renderCounter = 2;
	}
	
	private void drawChunk(Chunk chunk, BufferedImage img, Graphics g, 
							CameraTransform bufferTransform) {
		chunk.setRenderCounter(renderCounter);
		Point chunkPixelPos = bufferTransform.toScreen(
				new Point(chunk.getChunkX() * 16, chunk.getChunkZ() * 16), buffer.getWidth(),
				buffer.getHeight());
		
		int chunkRes = img.getWidth();
		if(bufferTransform.zoomLevel > 4)
			chunkRes *= 1 << (bufferTransform.zoomLevel - 4);

		g.drawImage(img, chunkPixelPos.ix(), chunkPixelPos.iy(), chunkRes, chunkRes, null);
		
		// Draw the height info
		
		if(bufferTransform.zoomLevel > 4) {
			int step = chunkRes / 16;
			int imgI;
			int imgJ = chunkPixelPos.iy();
			int height = 0;
			for(int j = 0; j < 16; ++j) {
				imgI = chunkPixelPos.ix();
				for(int i = 0; i < 16; ++i) {
					height = (chunk.getHeightLocalNoLoad(i, j) + 1024) | 0xFF000000;
					for(int jj = 0; jj < step; ++jj) {
						if((imgJ + jj) < 0 || (imgJ + jj) >= heightBuffer.getHeight())
							continue;
						for(int ii = 0; ii < step; ++ii) {
							if((imgI + ii) < 0 || (imgI + ii) >= heightBuffer.getWidth())
								continue;
							heightBuffer.setRGB(imgI + ii, imgJ + jj, height);
						}
					}
					
					imgI += step;
				}
				imgJ += step;
			}
		}else {
			int step = 16 / chunkRes;
			int imgI;
			int imgJ = chunkPixelPos.iy();
			int height = 0;
			for(int j = 0; j < 16; j += step) {
				if(imgJ < 0 || imgJ >= heightBuffer.getHeight()) {
					imgJ++;
					continue;
				}
				imgI = chunkPixelPos.ix();
				for(int i = 0; i < 16; i += step) {
					if(imgI < 0 || imgI >= heightBuffer.getWidth()) {
						imgI++;
						continue;
					}
					height = (chunk.getHeightLocalNoLoad(i, j) + 1024) | 0xFF000000;
					heightBuffer.setRGB(imgI, imgJ, height);
					imgI++;
				}
				imgJ++;
			}
		}
	}
	
	private void reprojectBuffer(BufferedImage buffer, Graphics2D g, 
			CameraTransform newBufferTransform, CameraTransform oldBufferTransform) {
		if(newBufferTransform.equals(oldBufferTransform))
			return; // No need to do any reprojection is the transform didn't change.
		
		if(newBufferTransform.zoomLevel == oldBufferTransform.zoomLevel) {
			// Zoom level is still the same, so all we're doing is moving pixels over.
			// To avoid having to reload and re-render lots of chunks, we find all
			// chunks that are fully within the old buffer and then set the chunk's
			// render counter to the current render counter to tell it that the chunk's
			// already properly rendered out.
			
			try {
				Point minBlock = bufferTransform.toWorld(new Point(0, 0), buffer.getWidth(), buffer.getHeight());
				Point maxBlock = bufferTransform.toWorld(new Point(buffer.getWidth(), buffer.getHeight()),
						buffer.getWidth(), buffer.getHeight());
	
				minChunkX = minBlock.ix() >> 4 + 1;
				minChunkZ = minBlock.iy() >> 4 + 1;
				maxChunkX = maxBlock.ix() >> 4 - 1;
				maxChunkZ = maxBlock.iy() >> 4 - 1;
	
				for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
					for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
						Chunk chunk = MCWorldExporter.getApp().getWorld().getChunk(chunkX, chunkZ);
						if (chunk == null)
							continue;
						
						// Only update the render counter if it was rendered the previous frame.
						if(chunk.getRenderCounter() == (renderCounter - 1))
							chunk.setRenderCounter(renderCounter);
					}
				}
			}catch(Exception ex) {
				World.handleError(ex);
			}
		}
		
		if(tmpBuffer == null || tmpBuffer.getWidth() != buffer.getWidth() || 
				tmpBuffer.getHeight() != buffer.getHeight()) {
			tmpBuffer = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB);
		}
		
		// First copy the buffer into tmpBuffer;
		Graphics2D gTmp = (Graphics2D) tmpBuffer.getGraphics();
		gTmp.drawImage(buffer, 0, 0, null);
		
		// Now draw the tmpBuffer into the buffer.
		g.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());
		
		double zoom = Math.pow(2.0, newBufferTransform.zoomLevel - oldBufferTransform.zoomLevel);
		int bufferWidth = (int) (((double) buffer.getWidth()) * zoom);
		int bufferHeight = (int) (((double) buffer.getHeight()) * zoom);
		Point bufferOffset = newBufferTransform.getPixelDifference(oldBufferTransform, 
				buffer.getWidth(), buffer.getHeight(), bufferWidth, bufferHeight);
		g.drawImage(tmpBuffer, bufferOffset.ix(), bufferOffset.iy(), bufferWidth, bufferHeight, null);
	}

	@Override
	public void run() {
		long lastTime = System.currentTimeMillis();
		while (true) {
			try {
				// Calculate for how long to sleep so that this loop
				// is run once every 16ms.
				Thread.sleep(Math.max(16 - (System.currentTimeMillis() - lastTime), 0));
				lastTime = System.currentTimeMillis();
				// If there isn't a render requested, skip.
				boolean fullRender = renderRequested.getAndSet(false);
				boolean clearBuffer = this.clearBuffer.getAndSet(false);
				if(clearBuffer)
					fullRender = true;
				
				if(!fullRender && finishedChunksStack.isEmpty())
					continue;
				if(bufferWidth <= 0 || bufferHeight <= 0)
					continue;
				if(MCWorldExporter.getApp().getWorld() != null) {
					if(MCWorldExporter.getApp().getWorld().isPaused())
						continue;
				}

				// Render to the buffer
				renderCounter++;

				// Make sure that the buffer is the right size.
				if (buffer == null || buffer.getWidth() != bufferWidth || buffer.getHeight() != bufferHeight) {
					buffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);
					heightBuffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);
					fullRender = true;
					clearBuffer = true;
				}
				// Copy the bufferTranform pointer, in case it gets updated during rendering.
				CameraTransform bufferTransform = this.bufferTransform;

				// Setup the Graphics2D and clear the buffer
				Graphics2D g = (Graphics2D) buffer.getGraphics();

				g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
						RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
				g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);

				if(fullRender) {
					if(clearBuffer)
						g.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());
					else
						reprojectBuffer(buffer, g, bufferTransform, frontBufferTransform);
				}
				
				Graphics2D gHeight = (Graphics2D) heightBuffer.getGraphics();

				gHeight.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
						RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
				gHeight.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				gHeight.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
				gHeight.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
				gHeight.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				gHeight.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				gHeight.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);

				if(fullRender) {
					if(clearBuffer)
						gHeight.clearRect(0, 0, heightBuffer.getWidth(), heightBuffer.getHeight());
					else
						reprojectBuffer(heightBuffer, gHeight, bufferTransform, frontBufferTransform);
				}

				// If there isn't a world to render, stop here.
				if (MCWorldExporter.getApp().getWorld() == null)
					continue;

				Point minBlock = bufferTransform.toWorld(new Point(0, 0), buffer.getWidth(), buffer.getHeight());
				Point maxBlock = bufferTransform.toWorld(new Point(buffer.getWidth(), buffer.getHeight()),
						buffer.getWidth(), buffer.getHeight());

				// Render chunks and optionally schedule the loading of chunks.
				if (fullRender) {
					minChunkX = minBlock.ix() >> 4;
					minChunkZ = minBlock.iy() >> 4;
					maxChunkX = maxBlock.ix() >> 4;
					maxChunkZ = maxBlock.iy() >> 4;

					for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
						for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
							Chunk chunk = MCWorldExporter.getApp().getWorld().getChunk(chunkX, chunkZ);
							if (chunk == null)
								continue;

							BufferedImage img = chunk.getChunkImageForZoomLevel(bufferTransform.zoomLevel);
							// If the image is null and the render counter of the chunk doesn't match,
							// then draw the chunk. If the chunk's shouldRender flag is set, force the render.
							if ((img == null && chunk.getRenderCounter() != renderCounter) || chunk.getShouldRender()) {
								// Render chunk
								chunk.setShouldRender(false);
								if (!chunk.hasLoadError() && !chunk.getRenderRequested()) {
									chunk.setRenderRequested(true);
									threadPool.submit(new LoadChunkTask(chunk, this));
								}
							}
							// If we don't have an image to show, skip
							if (img == null)
								continue;

							drawChunk(chunk, img, g, bufferTransform);
						}
					}
				}

				// Also render out chunks that have been loaded so far.
				synchronized (finishedChunksStack) {
					for (Chunk chunk : finishedChunksStack) {
						if (chunk == null)
							continue;

						BufferedImage img = chunk.getChunkImageForZoomLevel(bufferTransform.zoomLevel);

						// If we don't have an image to show, skip
						if (img == null)
							continue;

						drawChunk(chunk, img, g, bufferTransform);
					}
					finishedChunksStack.clear();
				}
				
				// Do post process
				if (postBuffer == null || postBuffer.getWidth() != buffer.getWidth()
						|| postBuffer.getHeight() != buffer.getHeight()) {
					postBuffer = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB);
				}
				// Process the heightmap and copy over the colours from buffer into frontBuffer
				for(int j = 0; j < heightBuffer.getHeight(); ++j) {
					for(int i = 0; i < heightBuffer.getWidth(); ++i) {
						int centre = (heightBuffer.getRGB(i, j) & 0x00FFFFFF) - 1024;
						int left = (heightBuffer.getRGB(Math.max(i - 1, 0), j) & 0x00FFFFFF) - 1024;
						int right = (heightBuffer.getRGB(Math.min(i + 1, heightBuffer.getWidth()-1), j) & 0x00FFFFFF) - 1024;
						int up = (heightBuffer.getRGB(i, Math.min(j + 1, heightBuffer.getHeight()-1)) & 0x00FFFFFF) - 1024;
						int down = (heightBuffer.getRGB(i, Math.max(j - 1, 0)) & 0x00FFFFFF) - 1024;
						left = centre - left;
						right = centre - right;
						up = centre - up;
						down = centre - down;
						
						// Increase the difference from one side,
						// so that we can see direction better.
						right = right * 4;
						down = down * 4;
						
						int total = left + right + up + down;
						total = total > 0 ? 0 : total;
						
						float ftotal = (float) (0.5 + 1.0 / (1.0 + Math.exp(-0.25 * ((double) total))));
						
						int colour = buffer.getRGB(i, j);
						Color color = new Color(colour);
						color = new Color(	Math.min((int) (((float) color.getRed()) * ftotal), 255),
											Math.min((int) (((float) color.getGreen()) * ftotal), 255), 
											Math.min((int) (((float) color.getBlue()) * ftotal), 255));
						colour = color.getRGB();
						postBuffer.setRGB(i, j, colour);
					}
				}

				// Swap the buffer
				frontBufferLock.aqcuire();
				
				BufferedImage tmp = frontBuffer;
				frontBuffer = postBuffer;
				postBuffer = tmp;
				
				frontBufferTransform = bufferTransform;
				frontBufferLock.release();

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void requestRender() {
		renderRequested.set(true);
	}
	
	public void clearBuffer() {
		clearBuffer.set(true);
	}

	public void setResolution(int width, int height) {
		if (bufferWidth == width && bufferHeight == height)
			return;
		bufferWidth = width;
		bufferHeight = height;
		requestRender();
	}

	public void setCameraTransform(CameraTransform transform) {
		bufferTransform = transform.clone();
		requestRender();
	}

	public SpinLock getFrameBufferLock() {
		return frontBufferLock;
	}

	public BufferedImage getFrameBuffer() {
		return frontBuffer;
	}

	public CameraTransform getCameraTransform() {
		return frontBufferTransform;
	}

	private static class LoadChunkTask implements Runnable {

		private Chunk chunk;
		private Renderer2D renderer;

		public LoadChunkTask(Chunk chunk, Renderer2D viewer) {
			this.chunk = chunk;
			this.renderer = viewer;
		}

		@Override
		public void run() {
			MCWorldExporter.worldMutex.acquireRead();
			if(chunk.getRegion().getWorld() != MCWorldExporter.getApp().getWorld()) {
				MCWorldExporter.worldMutex.releaseRead();
				return;
			}
			try {
				if (chunk.getChunkX() < renderer.minChunkX || chunk.getChunkX() > renderer.maxChunkX
						|| chunk.getChunkZ() < renderer.minChunkZ || chunk.getChunkZ() > renderer.maxChunkZ) {
					// It's outside of what we can view, so we undo the render request.
					chunk.setRenderRequested(false);
					MCWorldExporter.worldMutex.releaseRead();
					return;
				}

				chunk.load();
				chunk.renderChunkImage();
				chunk.getChunkImage();
				
				synchronized(renderer.finishedChunksStack) {
					renderer.finishedChunksStack.add(chunk);
				}
			} catch (Exception e) {
				World.handleError(e);
			}
			MCWorldExporter.worldMutex.releaseRead();
		}

	}

}
