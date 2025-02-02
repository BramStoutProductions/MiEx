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

package nl.bramstout.mcworldexporter.entity.builtins;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagDouble;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.resourcepack.EntityAIHandler;
import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;

public abstract class EntityHandlerHangable extends EntityHandler{

	@Override
	public void setup(Entity entity) {
		NbtTag posTag = entity.getProperties().get("Pos");
		if(posTag != null && posTag instanceof NbtTagList) {
			NbtTagList posList = (NbtTagList) posTag;
			NbtTag el0 = posList.get(0);
			if(el0 instanceof NbtTagFloat) {
				entity.setX(((NbtTagFloat) posList.get(0)).getData());
				entity.setY(((NbtTagFloat) posList.get(1)).getData());
				entity.setZ(((NbtTagFloat) posList.get(2)).getData());
			}else if(el0 instanceof NbtTagDouble) {
				entity.setX((float) ((NbtTagDouble) posList.get(0)).getData());
				entity.setY((float) ((NbtTagDouble) posList.get(1)).getData());
				entity.setZ((float) ((NbtTagDouble) posList.get(2)).getData());
			}
		}
		if(entity.getProperties().get("TileX") != null) {
			float blockX = ((NbtTagInt) entity.getProperties().get("TileX")).getData();
			float blockY = ((NbtTagInt) entity.getProperties().get("TileY")).getData();
			float blockZ = ((NbtTagInt) entity.getProperties().get("TileZ")).getData();
			entity.setX(blockX);
			entity.setY(blockY);
			entity.setZ(blockZ);
		}
		if(entity.getProperties().get("x") != null) {
			float blockX = ((NbtTagInt) entity.getProperties().get("x")).getData();
			float blockY = ((NbtTagInt) entity.getProperties().get("y")).getData();
			float blockZ = ((NbtTagInt) entity.getProperties().get("z")).getData();
			entity.setX(blockX);
			entity.setY(blockY);
			entity.setZ(blockZ);
		}
	}

	@Override
	public EntityAIHandler getAIHandler(Entity entity) {
		return null;
	}

}
