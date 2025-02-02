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
