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

import nl.bramstout.mcworldexporter.MCWorldExporter;
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
	
	private int regionSize;
	private int xPos;
	private int yPos;
	private int xOffset;
	private int yOffset;
	
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
		int xWorld = ((cursorX - xPos) * 32 * 16) / Math.max(regionSize,1) + (xOffset * 32 * 16);
		int zWorld = ((cursorY - yPos) * 32 * 16) / Math.max(regionSize,1) + (yOffset * 32 * 16);
		
		return new Point(xWorld, zWorld);
	}
	
	private void paintBuffer() {
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
		int regionWidth = regionMaxX - regionMinX + 1;
		int regionHeight = regionMaxZ - regionMinZ + 1;
		
		regionSize = Math.max(Math.min(getWidth() / regionWidth, getHeight() / regionHeight),1);
		int imgWidth = regionWidth * regionSize;
		int imgHeight = regionHeight * regionSize;
		
		xPos = (getWidth() - imgWidth) / 2;
		yPos = (getHeight() - imgHeight) / 2;
		xOffset = regionMinX;
		yOffset = regionMinZ;
		
		for(int regionZ = regionMinZ; regionZ <= regionMaxZ; ++regionZ) {
			for(int regionX = regionMinX; regionX <= regionMaxX; ++regionX) {
				Region region = world.getRegion(regionX << 5, regionZ << 5);
				if(region == null)
					continue;
				
				for(int chunkZ = 0; chunkZ < 32; ++chunkZ) {
					for(int chunkX = 0; chunkX < 32; ++chunkX) {
						try {
							Chunk chunk = region.getChunk((regionX << 5) + chunkX, (regionZ << 5) + chunkZ);
							if(chunk == null)
								continue;
							
							if(chunk instanceof ChunkAnvil) {
								if(((ChunkAnvil) chunk).getDataSize() <= 0)
									continue;
							}else if(chunk instanceof ChunkBedrock) {
								continue;
							}
							
							int xStart = (regionX-regionMinX) * regionSize + (chunkX * regionSize) / 32 + xPos;
							int yStart = (regionZ-regionMinZ) * regionSize + (chunkZ * regionSize) / 32 + yPos;
							int xEnd = (regionX-regionMinX) * regionSize + ((chunkX+1) * regionSize) / 32 + xPos;
							int yEnd = (regionZ-regionMinZ) * regionSize + ((chunkZ+1) * regionSize) / 32 + yPos;
							g2.fillRect(xStart, yStart, Math.max(xEnd-xStart,1), Math.max(yEnd-yStart,1));
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		}
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
