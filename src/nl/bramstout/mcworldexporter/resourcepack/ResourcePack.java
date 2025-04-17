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

package nl.bramstout.mcworldexporter.resourcepack;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.entity.spawning.EntitySpawner;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public abstract class ResourcePack {
	
	private String name;
	private String uuid;
	private File folder;
	private int worldVersion;
	
	public ResourcePack(String name, String uuid, File folder) {
		this.name = name;
		this.uuid = uuid;
		this.folder = folder;
		this.worldVersion = 0;
		File packInfoFile = new File(folder, "packInfo.json");
		if(packInfoFile.exists()) {
			try {
				JsonObject obj = Json.read(packInfoFile).getAsJsonObject();
				if(obj.has("worldVersion"))
					worldVersion = obj.get("worldVersion").getAsInt();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	public String getUUID() {
		return uuid;
	}
	
	public File getFolder() {
		return folder;
	}
	
	public abstract List<File> getFolders();
	
	public abstract List<File> getFoldersReversed();
	
	public int getWorldVersion() {
		return worldVersion;
	}
	
	public abstract void load();
	
	/**
	 * Gets the file in this resource pack with the given resource identifier.
	 * If this resource pack does not contain a file with this resource identifier,
	 * it returns a File instance pointing to a file that does not exist.
	 * 
	 * @param name The resource identifier
	 * @param type The resource type (blockstates, models, shaders, textures)
	 * @param category The category (assets, data, materials, translation)
	 * @param extension The file extension to use.
	 * @return A File instance pointing to the file
	 */
	public abstract File getResource(String name, String type, String category, String extension);
	
	public abstract File getTexture(String name);
	
	/**
	 * Returns a BlockStateHandler for the block with the given name.
	 * If this resource pack does not handle this block, it returns null.
	 * 
	 * @param name The resource identifier of the block.
	 * @return A BlockStateHandler instance for the block, or null if this resource pack doesn't handle it.
	 */
	public abstract BlockStateHandler getBlockStateHandler(String name);
	
	/**
	 * Returns a ModelHandler for the model with the given name.
	 * If this resource pack does not handle this model, it returns null.
	 * 
	 * @param name The resource identifier of the model.
	 * @return A ModelHandler instance for the model, or null if this resource pack doesn't handle it.
	 */
	public abstract ModelHandler getModelHandler(String name);
	
	/**
	 * Returns an EntityHandler for the entity with the given name.
	 * If this resource pack does not handle this entity, it returns null.
	 * 
	 * @param name The resource identifier of the entity.
	 * @return An EntityHandler instance for the entity, or null if this resource pack doesn't handle it.
	 */
	public abstract EntityHandler getEntityHandler(String name);
	
	/**
	 * Returns an EntityAIHandler for the entity with the given name.
	 * If this resource pack does not handle this entity, it returns null.
	 * 
	 * @param name The resource identifier of the entity.
	 * @return An EntityAIHandler instance for the entity, or null if this resource pack doesn't handle it.
	 */
	public abstract EntityAIHandler getEntityAIHandler(String name);
	
	/**
	 * Returns an ItemHandler for the item with the given name.
	 * If this resource pack does not handle this entity, it returns null.
	 * 
	 * @param name The resource identifier of the item.
	 * @param data The Nbt data of the item.
	 * @return An ItemHandler instance for the entity, or null if this resource pack doesn't handle it.
	 */
	public abstract ItemHandler getItemHandler(String name, NbtTagCompound data);
	
	public abstract Biome getBiome(String name, int id);
	
	public abstract void parseTags(Map<String, List<String>> tagToResourceIdentifiers);
	
	public abstract void parseBannerPatterns(Map<String, String> patternMap);
	
	public abstract MCMeta getMCMeta(String texture);
	
	public abstract Animation getAnimation(String animation);
	
	public abstract PaintingVariant getPaintingVariant(String id);
	
	public abstract void getEntitySpawners(List<EntitySpawner> spawners);
	
	public abstract Font getFont(String id);
	
	/**
	 * Get all textures in this resource pack that fit in the given groups.
	 * @param out A list of entries with the key being the resource identifier and the file instance of it. 
	 * @param groups
	 */
	public abstract void getTextures(List<Entry<String, File>> out, TextureGroup... groups);
	
}
