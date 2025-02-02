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

package nl.bramstout.mcworldexporter.pbr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.bramstout.mcworldexporter.pbr.nodes.PbrAttribute;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;

public class PbrContext {
	
	public String texture;
	public String textureExtension;
	public ResourcePack resourcePack;
	public List<ResourcePack> resourcePacks = new ArrayList<ResourcePack>();
	public ResourcePack saveToResourcePack = null;
	
	public Map<PbrAttribute, Object> valueCache = new HashMap<PbrAttribute, Object>();
	public Set<PbrAttribute> dirtyAttributes = new HashSet<PbrAttribute>();
	
	public List<File> temporaryFiles = new ArrayList<File>();
	
	public File getFile(String resource, String type, String extension, String category) {
		ResourcePack pack = null;
		for(int i = 0; i < resourcePacks.size(); ++i) {
			pack = resourcePacks.get(i);
			File file = pack.getResource(resource, type, category, extension);
			if(file != null && file.exists())
				return file;
		}
		return null;
	}
	
	public File getTexture(String id, boolean forceSameResourcepack, boolean isSave) {
		String extension = null;
		int extensionIndex = id.lastIndexOf((int) '.');
		if(extensionIndex >= 0) {
			extension = id.substring(extensionIndex);
			id = id.substring(0, extensionIndex);
		}
		
		ResourcePack forceRp = null;
		if(id.startsWith("{")) {
			String rpName = id.substring(1, id.indexOf('}'));
			id = id.substring(id.indexOf('}') + 1);
			for(int i = 0; i < resourcePacks.size(); ++i) {
				if(resourcePacks.get(i).getUUID().equals(rpName)) {
					forceRp = resourcePacks.get(i);
					break;
				}
			}
		}
		
		if(isSave) {
			if(saveToResourcePack != null) {
				id = id.replace("@texture@", texture);
				return getTexture(id, extension, saveToResourcePack);
			}
		}
		if(forceRp != null) {
			id = id.replace("@texture@", texture);
			return getTexture(id, extension, forceRp);
		}
		
		if(id.equals("@texture@"))
			return getTexture(texture, textureExtension, resourcePack);
		id = id.replace("@texture@", texture);
		if(forceSameResourcepack) {
			return getTexture(id, extension, resourcePack);
		}else {
			for(int i = 0; i < resourcePacks.size(); ++i) {
				File file = getTexture(id, extension, resourcePacks.get(i));
				if(file.exists())
					return file;
			}
			return getTexture(id, extension, resourcePack);
		}
	}
	
	private File getTexture(String id, String extension, ResourcePack resourcePack) {
		if(extension != null) {
			return resourcePack.getResource(id, "textures", "assets", extension);
		}else {
			File file = resourcePack.getResource(id, "textures", "assets", ".exr");
			if(file.exists())
				return file;
			file = resourcePack.getResource(id, "textures", "assets", ".tga");
			if(file.exists())
				return file;
			file = resourcePack.getResource(id, "textures", "assets", ".png");
			return file;
		}
	}
	
}
