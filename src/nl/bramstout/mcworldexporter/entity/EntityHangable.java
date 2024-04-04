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

package nl.bramstout.mcworldexporter.entity;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Byte;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_Int;

public abstract class EntityHangable extends Entity{

	protected Direction facing;
	
	public EntityHangable(String name, TAG_Compound properties) {
		super(name, properties);
		
		this.blockX = ((TAG_Int) properties.getElement("TileX")).value;
		this.blockY = ((TAG_Int) properties.getElement("TileY")).value;
		this.blockZ = ((TAG_Int) properties.getElement("TileZ")).value;
		this.x = blockX;
		this.y = blockY;
		this.z = blockZ;

		NBT_Tag facingTag = properties.getElement("Facing");
		if(facingTag == null)
			facingTag = properties.getElement("facing");
		byte facingByte = ((TAG_Byte) facingTag).value;
		if(facingByte == 0)
			facing = Direction.DOWN;
		else if(facingByte == 1)
			facing = Direction.UP;
		else if(facingByte == 2)
			facing = Direction.NORTH;
		else if(facingByte == 3)
			facing = Direction.SOUTH;
		else if(facingByte == 4)
			facing = Direction.WEST;
		else if(facingByte == 5)
			facing = Direction.EAST;
	}
	
	public Direction getFacing() {
		return facing;
	}

}
