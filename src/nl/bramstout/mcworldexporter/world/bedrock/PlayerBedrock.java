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

package nl.bramstout.mcworldexporter.world.bedrock;

import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.world.Player;

public class PlayerBedrock extends Player{

	public PlayerBedrock(String uuid, NbtTagCompound data) {
		super(uuid, uuid, data, 0, 0, 0, getDimension(data), false);
		
		NbtTag posTag = data.get("Pos");
		if(posTag != null && posTag instanceof NbtTagList) {
			if(((NbtTagList) posTag).getSize() >= 3) {
				NbtTag xTag = ((NbtTagList)posTag).get(0);
				NbtTag yTag = ((NbtTagList)posTag).get(1);
				NbtTag zTag = ((NbtTagList)posTag).get(2);
				this.x = xTag.asFloat();
				this.y = yTag.asFloat();
				this.z = zTag.asFloat();
			}
		}
	}
	
	private static String getDimension(NbtTagCompound data) {
		NbtTag dimensionIdTag = data.get("DimensionId");
		if(dimensionIdTag == null)
			return "overworld";
		if(dimensionIdTag instanceof NbtTagInt) {
			int id = ((NbtTagInt) dimensionIdTag).getData();
			if(id == 0)
				return "overworld";
			else if(id == 1)
				return "the_nether";
			else if(id == 2)
				return "the_end";
		}
		return "overworld";
	}

}
