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

package nl.bramstout.mcworldexporter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.export.LargeDataInputStream;
import nl.bramstout.mcworldexporter.export.LargeDataOutputStream;

public class ExportBounds {
	
	public static class ExcludeRegion{
		public int minX;
		public int minY;
		public int minZ;
		public int maxX;
		public int maxY;
		public int maxZ;
		
		public ExcludeRegion() {
			minX = 0;
			minY = 0;
			minZ = 0;
			maxX = 0;
			maxY = 0;
			maxZ = 0;
		}
		
		public ExcludeRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxY = maxY;
			this.maxZ = maxZ;
		}
		
		public ExcludeRegion(ExcludeRegion other) {
			minX = other.minX;
			minY = other.minY;
			minZ = other.minZ;
			maxX = other.maxX;
			maxY = other.maxY;
			maxZ = other.maxZ;
		}
		
		@Override
		public String toString() {
			return "[ " + Integer.toString(minX) + ", " + 
					Integer.toString(minY) + ", " + 
					Integer.toString(minZ) + ", " + 
					Integer.toString(maxX) + ", " + 
					Integer.toString(maxY) + ", " + 
					Integer.toString(maxZ) + " ]"; 
		}
	}
	
	public String name;
	public String safeName;
	public int minX;
	public int minY;
	public int minZ;
	public int maxX;
	public int maxY;
	public int maxZ;
	public int offsetX;
	public int offsetY;
	public int offsetZ;
	public int lodCenterX;
	public int lodCenterZ;
	public int lodWidth;
	public int lodDepth;
	public int lodYDetail;
	public int lodBaseLevel;
	public boolean enableLOD;
	public List<Pair<Integer, Integer>> disabledChunks;
	public List<String> fgChunks;
	public int chunkSize;
	public List<ExcludeRegion> excludeRegions;
	public boolean excludeRegionsAsAir;
	public boolean onlyIndividualBlocks;
	
	public ExportBounds(String name) {
		this.name = name;
		generateSafeName();
		minX = 0;
		minY = -64;
		minZ = 0;
		maxX = 0;
		maxY = 320;
		maxZ = 0;
		offsetX = 0;
		offsetY = 0;
		offsetZ = 0;
		lodCenterX = 0;
		lodCenterZ = 0;
		lodWidth = 0;
		lodDepth = 0;
		lodYDetail = 4;
		lodBaseLevel = 0;
		enableLOD = false;
		disabledChunks = new ArrayList<Pair<Integer, Integer>>();
		fgChunks = new ArrayList<String>();
		chunkSize = Config.chunkSize;
		excludeRegions = new ArrayList<ExcludeRegion>();
		excludeRegionsAsAir = false;
		onlyIndividualBlocks = false;
	}
	
	public ExportBounds(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, 
						int offsetX, int offsetY, int offsetZ, int lodCenterX, int lodCenterZ, 
						int lodWidth, int lodDepth, int lodYDetail, int lodBaseLevel, boolean enableLOD, 
						List<Pair<Integer, Integer>> disabledChunks, List<String> fgChunks,
						int chunkSize, List<ExcludeRegion> excludeRegions, boolean excludeRegionsAsAir,
						boolean onlyIndividualBlocks) {
		this.name = name;
		generateSafeName();
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.lodCenterX = lodCenterX;
		this.lodCenterZ = lodCenterZ;
		this.lodWidth = lodWidth;
		this.lodDepth = lodDepth;
		this.lodYDetail = lodYDetail;
		this.lodBaseLevel = lodBaseLevel;
		this.enableLOD = enableLOD;
		this.disabledChunks = new ArrayList<Pair<Integer, Integer>>();
		for(Pair<Integer, Integer> chunk : disabledChunks)
			this.disabledChunks.add(new Pair<Integer, Integer>(chunk.getKey(), chunk.getValue()));
		this.fgChunks = new ArrayList<String>(fgChunks);
		this.chunkSize = chunkSize;
		this.excludeRegions = new ArrayList<ExcludeRegion>();
		for(ExcludeRegion excludeRegion : excludeRegions) {
			this.excludeRegions.add(new ExcludeRegion(excludeRegion));
		}
		this.excludeRegionsAsAir = excludeRegionsAsAir;
		this.onlyIndividualBlocks = onlyIndividualBlocks;
	}
	
	public boolean isInExcludeRegion(int x, int y, int z) {
		for(int i = 0; i < excludeRegions.size(); ++i) {
			ExcludeRegion region = excludeRegions.get(i);
			if(x >= region.minX && x <= region.maxX &&
					y >= region.minY && y <= region.maxY &&
					z >= region.minZ && z <= region.maxZ)
				return true;
		}
		return false;
	}
	
	private void generateSafeName() {
		safeName = name.toLowerCase();
		for(int i = 0; i < safeName.length(); ++i) {
			int codePoint = safeName.codePointAt(i);
			if(!(Character.isLetter(codePoint) || Character.isDigit(codePoint)) || codePoint > 127) {
				// Invalid character, so replace with underscore
				safeName = safeName.substring(0, i) + "_" + safeName.substring(i+1);
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getSafeName() {
		return safeName;
	}
	
	public int getMinX() {
		return minX;
	}
	
	public int getMinY() {
		return minY;
	}
	
	public int getMinZ() {
		return minZ;
	}
	
	public int getMaxX() {
		return maxX;
	}
	
	public int getMaxY() {
		return maxY;
	}
	
	public int getMaxZ() {
		return maxZ;
	}
	
	public int getOffsetX() {
		return offsetX;
	}
	
	public int getOffsetY() {
		return offsetY;
	}
	
	public int getOffsetZ() {
		return offsetZ;
	}
	
	public int getWidth() {
		return maxX - minX;
	}
	
	public int getHeight() {
		return maxY - minY;
	}
	
	public int getDepth() {
		return maxZ - minZ;
	}
	
	public int getLodCenterX() {
		return lodCenterX;
	}

	public int getLodCenterZ() {
		return lodCenterZ;
	}
	
	public int getLodWidth() {
		return lodWidth;
	}

	public int getLodDepth() {
		return lodDepth;
	}
	
	public int getLodYDetail() {
		return lodYDetail;
	}
	
	public int getLodBaseLevel() {
		return lodBaseLevel;
	}
	
	public int getLodMinX() {
		return lodCenterX - lodWidth / 2;
	}
	
	public int getLodMaxX() {
		return lodCenterX + ((int) Math.ceil(((double) lodWidth) / 2.0));
	}
	
	public int getLodMinZ() {
		return lodCenterZ - lodDepth / 2;
	}
	
	public int getLodMaxZ() {
		return lodCenterZ + ((int) Math.ceil(((double) lodDepth) / 2.0));
	}
	
	public boolean hasLod() {
		return enableLOD;
	}
	
	public List<String> getFgChunks(){
		return fgChunks;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public List<ExcludeRegion> getExcludeRegions(){
		return excludeRegions;
	}
	
	public boolean isExcludeRegionsAsAir() {
		return excludeRegionsAsAir;
	}
	
	public boolean isOnlyIndividualBlocks() {
		return onlyIndividualBlocks;
	}
	
	public void setName(String name) {
		// Make sure that we have a unique name
		if(MCWorldExporter.getApp().hasExportBounds(name) && !name.equals(this.name)) {
			String baseName = name;
			for(int i = baseName.length()-1; i >= 0; i--) {
				if(!Character.isDigit(baseName.codePointAt(i))) {
					baseName = baseName.substring(0, i+1);
					break;
				}
			}
			for(int i = 1; i < 10000; i++) {
				name = baseName + Integer.toString(i);
				if(!MCWorldExporter.getApp().hasExportBounds(name))
					break;
			}
		}
		this.name = name;
		generateSafeName();
	}

	public void set(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		this.minX = Math.min(minX, maxX);
		this.minY = Math.min(minY, maxY);
		this.minZ = Math.min(minZ, maxZ);
		this.maxX = Math.max(minX, maxX);
		this.maxY = Math.max(minY, maxY);
		this.maxZ = Math.max(minZ, maxZ);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void snapToChunks() {
		// Max values are -1 because these coordinates are inclusive
		minX = ((minX+8) >> 4) << 4;
		minZ = ((minZ+8) >> 4) << 4;
		maxX = ((maxX+8) >> 4) << 4 - 1;
		maxZ = ((maxZ+8) >> 4) << 4 - 1;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void snapToChunksMinX() {
		minX = ((minX+8) >> 4) << 4;
		MCWorldExporter.getApp().getUI().update();
	}
	public void snapToChunksMinZ() {
		minZ = ((minZ+8) >> 4) << 4;
		MCWorldExporter.getApp().getUI().update();
	}
	public void snapToChunksMaxX() {
		maxX = ((maxX+8) >> 4) << 4 - 1;
		MCWorldExporter.getApp().getUI().update();
	}
	public void snapToChunksMaxZ() {
		maxZ = ((maxZ+8) >> 4) << 4 - 1;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMinX(int minX) {
		this.minX = Math.min(minX, maxX);
		this.maxX = Math.max(minX, maxX);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMinY(int minY) {
		this.minY = Math.min(minY, maxY);
		this.maxY = Math.max(minY, maxY);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMinZ(int minZ) {
		this.minZ = Math.min(minZ, maxZ);
		this.maxZ = Math.max(minZ, maxZ);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMaxX(int maxX) {
		this.minX = Math.min(minX, maxX);
		this.maxX = Math.max(minX, maxX);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMaxY(int maxY) {
		this.minY = Math.min(minY, maxY);
		this.maxY = Math.max(minY, maxY);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMaxZ(int maxZ) {
		this.minZ = Math.min(minZ, maxZ);
		this.maxZ = Math.max(minZ, maxZ);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void includePoint(int x, int y, int z) {
		minX = Math.min(minX, x);
		minY = Math.min(minY, y);
		minZ = Math.min(minZ, z);
		maxX = Math.max(maxX, x);
		maxY = Math.max(maxY, y);
		maxZ = Math.max(maxZ, z);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setOffsetX(int offsetX) {
		this.offsetX = offsetX;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setOffsetY(int offsetY) {
		this.offsetY = offsetY;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setOffsetZ(int offsetZ) {
		this.offsetZ = offsetZ;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setLodCenterX(int lodCenterX) {
		this.lodCenterX = lodCenterX;
		MCWorldExporter.getApp().getUI().update();
	}

	public void setLodCenterZ(int lodCenterZ) {
		this.lodCenterZ = lodCenterZ;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setLodWidth(int lodWidth) {
		this.lodWidth = lodWidth;
		MCWorldExporter.getApp().getUI().update();
	}

	public void setLodDepth(int lodDepth) {
		this.lodDepth = lodDepth;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setLodYDetail(int lodYDetail) {
		this.lodYDetail = lodYDetail;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setLodBaseLevel(int lodBaseLevel) {
		this.lodBaseLevel = lodBaseLevel;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setLod(int minX, int minZ, int maxX, int maxZ) {
		int minX2 = Math.min(minX, maxX);
		int minZ2 = Math.min(minZ, maxZ);
		int maxX2 = Math.max(minX, maxX);
		int maxZ2 = Math.max(minZ, maxZ);
		
		minX = Math.max(minX2, this.minX);
		minZ = Math.max(minZ2, this.minZ);
		maxX = Math.min(maxX2, this.maxX);
		maxZ = Math.min(maxZ2, this.maxZ);
		lodWidth = Math.max(maxX - minX, 0);
		lodDepth = Math.max(maxZ - minZ, 0);
		lodCenterX = minX + lodWidth / 2;
		lodCenterZ = minZ + lodDepth / 2;
		enableLOD = true;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void disableLod() {
		enableLOD = false;
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void enableLod() {
		enableLOD = true;
		MCWorldExporter.getApp().getUI().update();
	}

	public int getCenterX() {
		return (minX + maxX) / 2;
	}
	
	public int getCenterZ() {
		return (minZ + maxZ) / 2;
	}
	
	public void enableAllChunks() {
		disabledChunks.clear();
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void disableChunk(int x, int y) {
		Pair<Integer, Integer> chunk = new Pair<Integer, Integer>(x, y);
		if(!disabledChunks.contains(chunk))
			disabledChunks.add(chunk);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void enableChunk(int x, int y) {
		Pair<Integer, Integer> chunk = new Pair<Integer, Integer>(x, y);
		disabledChunks.remove(chunk);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void toggleChunk(int x, int y) {
		Pair<Integer, Integer> chunk = new Pair<Integer, Integer>(x, y);
		if(disabledChunks.contains(chunk))
			disabledChunks.remove(chunk);
		else
			disabledChunks.add(chunk);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public boolean isChunkEnabled(int x, int y) {
		Pair<Integer, Integer> chunk = new Pair<Integer, Integer>(x, y);
		return !disabledChunks.contains(chunk);
	}
	
	public boolean isBlockInEnabledChunk(int x, int z) {
		int chunkStartX = getMinX() >> 4;
		int chunkStartZ = getMinZ() >> 4;
		int chunkSize = Config.chunkSize;
		int blockChunkX = x >> 4;
		int blockChunkZ = z >> 4;
		int exportChunkX = (blockChunkX - chunkStartX) / chunkSize;
		int exportChunkZ = (blockChunkZ - chunkStartZ) / chunkSize;
		return isChunkEnabled(exportChunkX, exportChunkZ);
	}
	
	public List<Pair<Integer, Integer>> getDisabledChunks(){
		return disabledChunks;
	}
	
	public void setDisabledChunks(List<Pair<Integer, Integer>> disabledChunks) {
		this.disabledChunks.clear();
		for(Pair<Integer, Integer> chunk : disabledChunks) {
			this.disabledChunks.add(new Pair<Integer, Integer>(chunk.getKey(), chunk.getValue()));
		}
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	public void setExcludeRegionsAsAir(boolean excludeRegionsAsAir) {
		this.excludeRegionsAsAir = excludeRegionsAsAir;
	}
	
	public void setOnlyIndividualBlocks(boolean onlyIndividualBlocks){
		this.onlyIndividualBlocks = onlyIndividualBlocks;
	}
	public ExportBounds copy() {
		return new ExportBounds(name, minX, minY, minZ, maxX, maxY, maxZ, offsetX, offsetY, offsetZ,
								lodCenterX, lodCenterZ, lodWidth, lodDepth, lodYDetail, lodBaseLevel, enableLOD, 
								disabledChunks, fgChunks, chunkSize, excludeRegions, excludeRegionsAsAir,
								onlyIndividualBlocks);
	}
	
	public void write(LargeDataOutputStream dos) throws IOException{
		dos.writeUTF(name);
		dos.writeInt(minX);
		dos.writeInt(minY);
		dos.writeInt(minZ);
		dos.writeInt(maxX);
		dos.writeInt(maxY);
		dos.writeInt(maxZ);
		dos.writeInt(offsetX);
		dos.writeInt(offsetY);
		dos.writeInt(offsetZ);
		dos.writeInt(lodCenterX);
		dos.writeInt(lodCenterZ);
		dos.writeInt(lodWidth);
		dos.writeInt(lodDepth);
		dos.writeInt(lodYDetail);
		dos.writeInt(lodBaseLevel);
		dos.writeBoolean(enableLOD);
		dos.writeInt(disabledChunks.size());
		for(int i = 0; i < disabledChunks.size(); ++i) {
			dos.writeInt(disabledChunks.get(i).getKey().intValue());
			dos.writeInt(disabledChunks.get(i).getValue().intValue());
		}
		dos.writeInt(fgChunks.size());
		for(int i = 0; i < fgChunks.size(); ++i)
			dos.writeUTF(fgChunks.get(i));
		dos.writeInt(chunkSize);
		dos.writeInt(excludeRegions.size());
		for(int i = 0; i < excludeRegions.size(); ++i) {
			dos.writeInt(excludeRegions.get(i).minX);
			dos.writeInt(excludeRegions.get(i).minY);
			dos.writeInt(excludeRegions.get(i).minZ);
			dos.writeInt(excludeRegions.get(i).maxX);
			dos.writeInt(excludeRegions.get(i).maxY);
			dos.writeInt(excludeRegions.get(i).maxZ);
		}
		dos.writeBoolean(excludeRegionsAsAir);
		dos.writeBoolean(onlyIndividualBlocks);
	}
	
	public void read(LargeDataInputStream dis) throws IOException{
		name = dis.readUTF();
		generateSafeName();
		minX = dis.readInt();
		minY = dis.readInt();
		minZ = dis.readInt();
		maxX = dis.readInt();
		maxY = dis.readInt();
		maxZ = dis.readInt();
		offsetX = dis.readInt();
		offsetY = dis.readInt();
		offsetZ = dis.readInt();
		lodCenterX = dis.readInt();
		lodCenterZ = dis.readInt();
		lodWidth = dis.readInt();
		lodDepth = dis.readInt();
		lodYDetail = dis.readInt();
		lodBaseLevel = dis.readInt();
		enableLOD = dis.readBoolean();
		int numDisabledChunks = dis.readInt();
		disabledChunks.clear();
		for(int i = 0; i < numDisabledChunks; ++i) {
			int x = dis.readInt();
			int z = dis.readInt();
			disabledChunks.add(new Pair<Integer, Integer>(x, z));
		}
		int numFgChunks = dis.readInt();
		fgChunks.clear();
		for(int i = 0; i < numFgChunks; ++i) {
			String fgChunk = dis.readUTF();
			fgChunks.add(fgChunk);
		}
		chunkSize = dis.readInt();
		int numExcludeRegions = dis.readInt();
		excludeRegions.clear();
		for(int i = 0; i < numExcludeRegions; ++i) {
			int minX = dis.readInt();
			int minY = dis.readInt();
			int minZ = dis.readInt();
			int maxX = dis.readInt();
			int maxY = dis.readInt();
			int maxZ = dis.readInt();
			excludeRegions.add(new ExcludeRegion(minX, minY, minZ, maxX, maxY, maxZ));
		}
		excludeRegionsAsAir = dis.readBoolean();
		onlyIndividualBlocks = dis.readBoolean();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean firstField = true;
		for(Field field : this.getClass().getFields()) {
			if(List.class.isAssignableFrom(field.getType())) {
				try {
					sb.append((firstField ? "" : "    ") + field.getName() + ":\n");
					firstField = false;
					List<?> data = (List<?>) field.get(this);
					for(Object obj : data) {
						sb.append("      " + obj.toString() + "\n");
					}
				}catch(Exception e) {
					e.printStackTrace();
				}
			}else{
				try {
					sb.append((firstField ? "" : "    ") + field.getName() + ": " + field.get(this).toString() + "\n");
					firstField = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}
	
}
