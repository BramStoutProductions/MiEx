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

package nl.bramstout.mcworldexporter.world.anvil;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_Double;
import nl.bramstout.mcworldexporter.nbt.TAG_List;
import nl.bramstout.mcworldexporter.nbt.TAG_String;
import nl.bramstout.mcworldexporter.world.Player;

public class PlayerAnvil extends Player{

	public PlayerAnvil(String uuid, File playerFile) {
		super(uuid, null, 0, 0, 0, null);
		try {
			GZIPInputStream is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(playerFile)));
			DataInputStream dis = new DataInputStream(is);
			NBT_Tag nbtData = NBT_Tag.make(dis);
			if(nbtData instanceof TAG_Compound) {
				this.data = (TAG_Compound) nbtData;
				
				NBT_Tag posTag = this.data.getElement("Pos");
				if(posTag != null && posTag instanceof TAG_List) {
					x = ((TAG_Double)((TAG_List)posTag).getElement(0)).value;
					y = ((TAG_Double)((TAG_List)posTag).getElement(1)).value;
					z = ((TAG_Double)((TAG_List)posTag).getElement(2)).value;
				}else {
					System.out.println("Could not get position for player " + this.name);
				}
				
				NBT_Tag dimensionTag = this.data.getElement("Dimension");
				if(dimensionTag != null && dimensionTag instanceof TAG_String) {
					this.dimension = ((TAG_String) dimensionTag).value;
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	
}
