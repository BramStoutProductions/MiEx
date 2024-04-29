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

public class ExportBounds {
	
	private int minX;
	private int minY;
	private int minZ;
	private int maxX;
	private int maxY;
	private int maxZ;
	private int offsetY;
	private int lodCenterX;
	private int lodCenterZ;
	private int lodWidth;
	private int lodDepth;
	private int lodYDetail;
	
	public ExportBounds() {
		minX = 0;
		minY = -64;
		minZ = 0;
		maxX = 0;
		maxY = 320;
		maxZ = 0;
		offsetY = 0;
		lodCenterX = 0;
		lodCenterZ = 0;
		lodWidth = 0;
		lodDepth = 0;
		lodYDetail = 4;
	}
	
	public ExportBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int offsetY,
						int lodCenterX, int lodCenterZ, int lodWidth, int lodDepth, int lodYDetail) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.offsetY = offsetY;
		this.lodCenterX = lodCenterX;
		this.lodCenterZ = lodCenterZ;
		this.lodWidth = lodWidth;
		this.lodDepth = lodDepth;
		this.lodYDetail = lodYDetail;
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
	
	public int getOffsetY() {
		return offsetY;
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
	
	public boolean isLodMinXConstraint() {
		return getLodMinX() <= minX;
	}
	
	public boolean isLodMaxXConstraint() {
		return getLodMaxX() >= maxX;
	}
	
	public boolean isLodMinZConstraint() {
		return getLodMinZ() <= minZ;
	}
	
	public boolean isLodMaxZConstraint() {
		return getLodMaxZ() >= maxZ;
	}
	
	public boolean hasLod() {
		return (lodDepth > 1 && lodWidth > 1) && !(
				isLodMinXConstraint() && isLodMaxXConstraint() &&
				isLodMinZConstraint() && isLodMaxZConstraint());
	}

	public void set(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		boolean constraintMinX = isLodMinXConstraint();
		boolean constraintMinZ = isLodMinZConstraint();
		boolean constraintMaxX = isLodMaxXConstraint();
		boolean constraintMaxZ = isLodMaxZConstraint();
		this.minX = Math.min(minX, maxX);
		this.minY = Math.min(minY, maxY);
		this.minZ = Math.min(minZ, maxZ);
		this.maxX = Math.max(minX, maxX);
		this.maxY = Math.max(minY, maxY);
		this.maxZ = Math.max(minZ, maxZ);
		if(constraintMinX)
			setLodMinX(minX);
		if(constraintMinZ)
			setLodMinZ(minZ);
		if(constraintMaxX)
			setLodMaxX(maxX);
		if(constraintMaxZ)
			setLodMaxZ(maxZ);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMinX(int minX) {
		boolean constraintMinX = isLodMinXConstraint();
		boolean constraintMaxX = isLodMaxXConstraint();
		this.minX = Math.min(minX, maxX);
		this.maxX = Math.max(minX, maxX);
		if(constraintMinX)
			setLodMinX(minX);
		if(constraintMaxX)
			setLodMaxX(maxX);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMinY(int minY) {
		this.minY = Math.min(minY, maxY);
		this.maxY = Math.max(minY, maxY);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMinZ(int minZ) {
		boolean constraintMinZ = isLodMinZConstraint();
		boolean constraintMaxZ = isLodMaxZConstraint();
		this.minZ = Math.min(minZ, maxZ);
		this.maxZ = Math.max(minZ, maxZ);
		if(constraintMinZ)
			setLodMinZ(minZ);
		if(constraintMaxZ)
			setLodMaxZ(maxZ);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMaxX(int maxX) {
		boolean constraintMinX = isLodMinXConstraint();
		boolean constraintMaxX = isLodMaxXConstraint();
		this.minX = Math.min(minX, maxX);
		this.maxX = Math.max(minX, maxX);
		if(constraintMinX)
			setLodMinX(minX);
		if(constraintMaxX)
			setLodMaxX(maxX);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMaxY(int maxY) {
		this.minY = Math.min(minY, maxY);
		this.maxY = Math.max(minY, maxY);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setMaxZ(int maxZ) {
		boolean constraintMinZ = isLodMinZConstraint();
		boolean constraintMaxZ = isLodMaxZConstraint();
		this.minZ = Math.min(minZ, maxZ);
		this.maxZ = Math.max(minZ, maxZ);
		if(constraintMinZ)
			setLodMinZ(minZ);
		if(constraintMaxZ)
			setLodMaxZ(maxZ);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void includePoint(int x, int y, int z) {
		boolean constraintMinX = isLodMinXConstraint();
		boolean constraintMinZ = isLodMinZConstraint();
		boolean constraintMaxX = isLodMaxXConstraint();
		boolean constraintMaxZ = isLodMaxZConstraint();
		minX = Math.min(minX, x);
		minY = Math.min(minY, y);
		minZ = Math.min(minZ, z);
		maxX = Math.max(maxX, x);
		maxY = Math.max(maxY, y);
		maxZ = Math.max(maxZ, z);
		if(constraintMinX)
			setLodMinX(minX);
		if(constraintMinZ)
			setLodMinZ(minZ);
		if(constraintMaxX)
			setLodMaxX(maxX);
		if(constraintMaxZ)
			setLodMaxZ(maxZ);
		MCWorldExporter.getApp().getUI().update();
	}
	
	public void setOffsetY(int offsetY) {
		this.offsetY = offsetY;
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
	}
	
	private void setLodMinX(int minX) {
		setLod2(minX, getLodMinZ(), getLodMaxX(), getLodMaxZ());
	}
	
	private void setLodMaxX(int maxX) {
		setLod2(getLodMinX(), getLodMinZ(), maxX, getLodMaxZ());
	}
	
	private void setLodMinZ(int minZ) {
		setLod2(getLodMinX(), minZ, getLodMaxX(), getLodMaxZ());
	}
	
	private void setLodMaxZ(int maxZ) {
		setLod2(getLodMinX(), getLodMinZ(), getLodMaxX(), maxZ);
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
		MCWorldExporter.getApp().getUI().update();
	}
	
	private void setLod2(int minX, int minZ, int maxX, int maxZ) {
		lodWidth = Math.max(maxX - minX, 0);
		lodDepth = Math.max(maxZ - minZ, 0);
		lodCenterX = minX + lodWidth / 2;
		lodCenterZ = minZ + lodDepth / 2;
	}
	
	public void disableLod() {
		setLod(minX, minZ, maxX, maxZ);
		MCWorldExporter.getApp().getUI().update();
	}

	public int getCenterX() {
		return (minX + maxX) / 2;
	}
	
	public int getCenterZ() {
		return (minZ + maxZ) / 2;
	}

	public ExportBounds copy() {
		return new ExportBounds(minX, minY, minZ, maxX, maxY, maxZ, offsetY,
								lodCenterX, lodCenterZ, lodWidth, lodDepth, lodYDetail);
	}
	
}
