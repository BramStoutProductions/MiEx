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

package nl.bramstout.mcworldexporter.world;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;

public class Block {
	
	private String name;
	private NbtTagCompound properties;
	private int id;
	private int dataVersion;
	private boolean waterlogged;
	private boolean liquid;
	private boolean isLiquid;
	
	public Block(String name, NbtTagCompound properties, int id, int dataVersion) {
		this.name = name;
		this.properties = properties;
		this.properties.acquireOwnership();
		this.id = id;
		this.dataVersion = dataVersion;
		this.isLiquid = Config.liquid.contains(name);
		waterlogged = false;
		NbtTagString waterloggedTag = (NbtTagString) properties.get("waterlogged");
		if(waterloggedTag != null)
			waterlogged = waterloggedTag.getData().equalsIgnoreCase("true");
		else if(Config.waterlogged.contains(name))
			waterlogged = true;
		if(waterlogged) {
			liquid = true;
		}else {
			liquid = isLiquid;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public NbtTagCompound getProperties() {
		return properties;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isWaterlogged() {
		return waterlogged;
	}
	
	public boolean isLiquid() {
		return isLiquid;
	}
	
	public boolean hasLiquid() {
		return liquid;
	}
	
	public int getDataVersion() {
		return dataVersion;
	}
}
