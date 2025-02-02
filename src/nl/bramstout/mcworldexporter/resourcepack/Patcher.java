package nl.bramstout.mcworldexporter.resourcepack;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.iq80.leveldb.shaded.guava.io.Files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.Pair;

public class Patcher {
	
	private static abstract class PatcherBase{
		
		public abstract void patch();
		
		protected List<Pair<File, String>> findFiles(String path){
			List<Pair<File, String>> res = new ArrayList<Pair<File, String>>();
			if(path.contains("*")) {
				int starIndex = path.indexOf((int) '*');
				String prefix = path.substring(0, starIndex);
				String suffix = path.substring(starIndex + 1);
				
				int slashIndex = prefix.lastIndexOf((int) '/');
				String parent = prefix.substring(0, slashIndex);
				prefix = prefix.substring(slashIndex + 1);
				
				File parentFolder = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + parent);
				if(!parentFolder.exists() || !parentFolder.isDirectory())
					return res;
				
				for(File f : parentFolder.listFiles()) {
					if(f.getName().startsWith(prefix) && f.getName().endsWith(suffix)) {
						String wildcardPart = f.getName().substring(prefix.length(), f.getName().length() - suffix.length());
						res.add(new Pair<File, String>(f, wildcardPart));
					}
				}
				
			}else {
				File file = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + path);
				if(file.exists())
					res.add(new Pair<File, String>(file, ""));
			}
			return res;
		}
		
	}
	
	private static class PatcherCopy extends PatcherBase{
		
		private String src;
		private String dst;
		
		public PatcherCopy(String src, String dst) {
			this.src = src;
			this.dst = dst;
		}
		
		@Override
		public void patch() {
			String dstPrefix = dst;
			String dstSuffix = "";
			int asterixIndex = dst.indexOf((int) '*');
			if(asterixIndex >= 0) {
				dstPrefix = dst.substring(0, asterixIndex);
				dstSuffix = dst.substring(asterixIndex + 1);
			}
			List<Pair<File, String>> files = findFiles(src);
			for(Pair<File, String> file : files) {
				String dstPath = dst;
				if(asterixIndex >= 0)
					dstPath = dstPrefix + file.getValue() + dstSuffix;
				File dstFile = new File(FileUtil.getResourcePackDir(), "base_resource_pack/" + dstPath);
				File parentFile = dstFile.getParentFile();
				if(!parentFile.exists())
					parentFile.mkdirs();
				try {
					Files.copy(file.getKey(), dstFile);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
	}
	
	private static class PatcherPatchJson extends PatcherBase{
		
		public static enum PatchMode{
			add, set
		}
		
		public static class JsonPatcher{
			
			private String parent;
			private PatchMode mode;
			private JsonElement data;
			
			public JsonPatcher(String parent, PatchMode mode, JsonElement data) {
				this.parent = parent;
				this.mode = mode;
				this.data = data;
			}
			
			public void patch(JsonElement root) {
				JsonElement parent = findParent(root, this.parent);
				
				// Make sure that both the parent and data are either both arrays or objects
				if(parent.isJsonArray() != data.isJsonArray() || parent.isJsonObject() != data.isJsonObject())
					return;
				
				if(parent.isJsonArray()) {
					if(mode == PatchMode.add) {
						parent.getAsJsonArray().addAll(data.getAsJsonArray().deepCopy());
					}else if(mode == PatchMode.set) {
						// Clear it.
						while(!parent.getAsJsonArray().isEmpty())
							parent.getAsJsonArray().remove(0);
						// Add the items.
						parent.getAsJsonArray().addAll(data.getAsJsonArray().deepCopy());
					}
				}else if(parent.isJsonObject()) {
					if(mode == PatchMode.add) {
						for(Entry<String, JsonElement> entry : data.getAsJsonObject().entrySet()) {
							parent.getAsJsonObject().add(entry.getKey(), entry.getValue().deepCopy());
						}
					}else if(mode == PatchMode.set) {
						// Remove all items.
						Set<String> keySet = new HashSet<String>(parent.getAsJsonObject().keySet());
						for(String key : keySet)
							parent.getAsJsonObject().remove(key);
						
						// Add the items.
						for(Entry<String, JsonElement> entry : data.getAsJsonObject().entrySet()) {
							parent.getAsJsonObject().add(entry.getKey(), entry.getValue().deepCopy());
						}
					}
				}
			}
			
			private JsonElement findParent(JsonElement root, String parentPath) {
				int slashIndex = parentPath.indexOf((int) '/');
				String childName = parentPath;
				if(slashIndex >= 0) {
					childName = parentPath.substring(0, slashIndex);
				}
				
				JsonElement parent = root;
				if(childName.length() > 0) {
					if(parent.isJsonArray()) {
						int index = 0;
						try {
							// For arrays we index child elements with the format [index]
							// so we substring the brackets away and then parse it as an integer.
							index = Integer.parseInt(childName.substring(1, childName.length()-1));
						}catch(Exception ex) {}
						if(index >= 0 && index < parent.getAsJsonArray().size()) {
							parent = parent.getAsJsonArray().get(index);
						}
					}else if(parent.isJsonObject()) {
						if(parent.getAsJsonObject().has(childName)) {
							parent = parent.getAsJsonObject().get(childName);
						}
					}
				}
				
				if(slashIndex >= 0) {
					// There was a slash, so we're not done traversing the parent path.
					return findParent(parent, parentPath.substring(slashIndex + 1));
				}else {
					// We are at the end of the parent path, so return parent
					return parent;
				}
			}
			
		}
		
		private String file;
		private List<JsonPatcher> patchers;
		
		public PatcherPatchJson(String file, List<JsonPatcher> patchers) {
			this.file = file;
			this.patchers = patchers;
		}
		
		@Override
		public void patch() {
			List<Pair<File, String>> files = findFiles(file);
			for(Pair<File, String> file2 : files) {
				try {
					JsonElement root = Json.read(file2.getKey());
					
					for(JsonPatcher patcher : patchers) {
						patcher.patch(root);
					}
					
					FileWriter writer = null;
					try {
						Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
						String jsonString = gson.toJson(root);
						writer = new FileWriter(file2.getKey());
						writer.write(jsonString);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					if(writer != null) {
						try {
							writer.close();
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
	}
	
	private static List<PatcherBase> patchers = new ArrayList<PatcherBase>();
	
	public static void load() {
		patchers.clear();
		
		File patchFile = new File(FileUtil.getResourcePackDir(), "base_resource_pack/miex_patches.json");
		if(!patchFile.exists())
			return;
		try {
			
			JsonArray patchers = Json.read(patchFile).getAsJsonArray();
			for(JsonElement el : patchers.asList()) {
				parsePatcher(el.getAsJsonObject());
			}
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void parsePatcher(JsonObject patcherData) {
		String type = "";
		if(patcherData.has("type")) {
			type = patcherData.get("type").getAsString();
		}
		
		if(type.equals("copy")) {
			String src = "";
			String dst = "";
			if(patcherData.has("src"))
				src = patcherData.get("src").getAsString();
			if(patcherData.has("dst"))
				dst = patcherData.get("dst").getAsString();
			
			if(!src.isEmpty() && !dst.isEmpty()) {
				patchers.add(new PatcherCopy(src, dst));
			}
		}else if(type.equals("patch_json")) {
			String file = "";
			List<PatcherPatchJson.JsonPatcher> jsonPatchers = new ArrayList<PatcherPatchJson.JsonPatcher>();
			if(patcherData.has("file"))
				file = patcherData.get("file").getAsString();
			if(patcherData.has("patches")) {
				for(JsonElement el : patcherData.getAsJsonArray("patches").asList()) {
					if(!el.isJsonObject())
						continue;
					String parent = "";
					PatcherPatchJson.PatchMode mode = null;
					JsonElement data = null;
					if(el.getAsJsonObject().has("parent"))
						parent = el.getAsJsonObject().get("parent").getAsString();
					if(el.getAsJsonObject().has("add")) {
						mode = PatcherPatchJson.PatchMode.add;
						data = el.getAsJsonObject().get("add");
					}
					if(el.getAsJsonObject().has("set")) {
						mode = PatcherPatchJson.PatchMode.set;
						data = el.getAsJsonObject().get("set");
					}
					if(mode != null && data != null)
						jsonPatchers.add(new PatcherPatchJson.JsonPatcher(parent, mode, data));
				}
			}
			if(!file.isEmpty() && !jsonPatchers.isEmpty()) {
				patchers.add(new PatcherPatchJson(file, jsonPatchers));
			}
		}
	}
	
	public static void patch() {
		for(PatcherBase patcher : patchers) {
			try {
				patcher.patch();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
