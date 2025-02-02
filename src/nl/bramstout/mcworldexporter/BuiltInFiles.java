package nl.bramstout.mcworldexporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.ui.BuiltInFilesDialog;

public class BuiltInFiles {
	
	public static void setupBuiltInFiles(boolean forceUpdate) {
		List<BuiltInFile> files = findBuiltInFiles();
		
		File manifestFile = new File(FileUtil.getResourcePackDir(), "manifest.json");
		JsonObject manifestObject = null;
		if(manifestFile.exists()) {
			JsonElement manifestData = Json.read(manifestFile);
			if(manifestData != null && manifestData.isJsonObject())
				manifestObject = manifestData.getAsJsonObject();
		}
		if(manifestObject == null)
			manifestObject = new JsonObject();
		
		List<BuiltInFile> modifiedByUser = new ArrayList<BuiltInFile>();
		
		for(BuiltInFile file : files) {
			String storedHash = "";
			
			JsonObject fileManifest = manifestObject.getAsJsonObject(file.name);
			if(fileManifest != null) {
				if(fileManifest.has("hash"))
					storedHash = fileManifest.get("hash").getAsString();
			}
			
			if(storedHash.equalsIgnoreCase("NO_OVERWRITE") && !forceUpdate)
				continue; // Easy way to tell MiEx to never overwrite this file.
			
			File actualFile = new File(FileUtil.getResourcePackDir(), file.name);
			
			
			// If the file doesn't exist, then always update it.
			if(storedHash.equals(file.hash) && !forceUpdate && actualFile.exists())
				continue; // Hash is the same, so no need to update the file
			
			
			// The hash is different, meaning that this built in file was changed,
			// so we need to update it.
			// First we want to check if the actual file has the same hash as the
			// stored hash from the manifest. If not, then the user changed the 
			// file at some point and we should ask the user first.
			String actualHash = "";
			if(actualFile.exists()) {
				InputStream is = null;
				try {
					is = new FileInputStream(actualFile);
					actualHash = calcHash(is, actualFile.length());
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				if(is != null) {
					try {
						is.close();
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			
			boolean wasModifiedByUser = !actualHash.equals(storedHash) && 
								!actualHash.isEmpty() && !storedHash.isEmpty();
			boolean shouldExtract = storedHash.isEmpty() || actualHash.isEmpty();
			if(forceUpdate)
				shouldExtract = !actualHash.equals(file.hash);
			
			if(wasModifiedByUser) {
				modifiedByUser.add(file);
			}else {
				if(!shouldExtract)
					continue;
				// We can easily update it now
				System.out.println("Installing " + file.name);
				try {
					File parentFile = actualFile.getParentFile();
					if(!parentFile.exists())
						parentFile.mkdirs();
					
					Files.copy(file.url.openStream(), actualFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					
					// Don't forget to update the file manifest
					if(fileManifest == null) {
						fileManifest = new JsonObject();
						manifestObject.add(file.name, fileManifest);
					}
					fileManifest.addProperty("hash", file.hash);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		if(!modifiedByUser.isEmpty()) {
			// Let the user choose what to update.
			BuiltInFilesDialog dialog = new BuiltInFilesDialog();
			dialog.show(modifiedByUser);
			
			for(BuiltInFile file : modifiedByUser) {
				JsonObject fileManifest = manifestObject.getAsJsonObject(file.name);
				if(fileManifest == null) {
					fileManifest = new JsonObject();
					manifestObject.add(file.name, fileManifest);
				}
				
				if(dialog.filesToChange.contains(file)) {
					// Change it
					File actualFile = new File(FileUtil.getResourcePackDir(), file.name);
					System.out.println("Installing " + file.name);
					try {
						File parentFile = actualFile.getParentFile();
						if(!parentFile.exists())
							parentFile.mkdirs();
						
						Files.copy(file.url.openStream(), actualFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						
						// Don't forget to update the file manifest
						fileManifest.addProperty("hash", file.hash);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}else {
					// Don't change it
					// Update the manifest with the hash of the file,
					// so that the user won't get asked every single time
					// they launch MiEx.
					fileManifest.addProperty("hash", file.hash);
				}
			}
		}
		
		Json.writeJson(manifestFile, manifestObject);
	}
	
	public static class BuiltInFile{
		
		public String name;
		public URL url;
		public String hash;
		
		public BuiltInFile(String name, URL url, String hash) {
			this.name = name;
			this.url = url;
			this.hash = hash;
		}
		
	}
	
	private static List<BuiltInFile> findBuiltInFiles(){
		List<BuiltInFile> files = new ArrayList<BuiltInFile>();
		
		// First check the GitHub repository for the latest built in files,
		// otherwise fall back to the files inside of the jar file.
		boolean gotFromGitHub = checkGitHubForBuiltInFiles(files);
		if(gotFromGitHub)
			return files;
		
		URL jarLocation = BuiltInFiles.class.getProtectionDomain().getCodeSource().getLocation();
		File jarFile = new File(jarLocation.getPath());
		if(jarFile.isDirectory()) {
			File defaultDataFolder = new File(jarFile, "default_data");
			if(defaultDataFolder.exists() && defaultDataFolder.isDirectory()) {
				searchFolder(defaultDataFolder, "", files);
			}
		}else if(jarFile.isFile()) {
			ZipInputStream zis = null;
			try {
				zis = new ZipInputStream(jarLocation.openStream());
				ZipEntry entry = null;
				while((entry = zis.getNextEntry()) != null) {
					if(entry.isDirectory())
						continue;
					if(entry.getName().startsWith("default_data/")) {
						try {
							String hash = calcHash(zis, entry.getSize());
							
							files.add(new BuiltInFile(
									entry.getName().substring("default_data/".length()), 
									BuiltInFiles.class.getClassLoader().getResource(entry.getName()), 
									hash));
						}catch(Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(zis != null) {
				try {
					zis.close();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		return files;
	}
	
	private static void searchFolder(File folder, String parent, List<BuiltInFile> files) {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				searchFolder(f, parent + f.getName() + "/", files);
			}else if(f.isFile()) {
				InputStream is = null;
				try {
					is = new FileInputStream(f);
					String hash = calcHash(is, f.length());
					
					files.add(new BuiltInFile(parent + f.getName(), f.toURI().toURL(), hash));
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				if(is != null) {
					try {
						is.close();
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	private static byte[] buffer = new byte[4096];
	private static byte[] buffer2 = new byte[4096];
	private static char[] bitsToHex = new char[] {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	private static String calcHash(InputStream is, long sizeInBytes) throws Exception{
		if(sizeInBytes > buffer.length) {
			buffer = new byte[(int) sizeInBytes];
			buffer2 = new byte[(int) sizeInBytes];
		}
		int read = 0;
		int totalRead = 0;
		while((read = is.read(buffer, totalRead, buffer.length - totalRead)) > 0) {
			totalRead += read;
		}
		boolean isBinary = false;
		int newSize = 0;
		// GitHub removes \r from text data so that there is just a \n.
		// So, we need to go through the data to do that as well.
		// However, it could be that it's binary, so if we come across
		// a byte that doesn't correspond to normal text, then we see it
		// as binary.
		for(int i = 0; i < totalRead; ++i) {
			if(buffer[i] == '\r')
				continue;
			if((buffer[i] <= 0x1F || buffer[i] == 0xFF) && buffer[i] != '\n') {
				// These bytes don't show up in text, so see this data
				// as binary data rather than text data.
				isBinary = true;
				break;
			}
			buffer2[newSize] = buffer[i];
			newSize++;
		}
		if(isBinary)
			newSize = totalRead;
		
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		
		md.update(("blob " + Integer.toString(newSize)).getBytes());
		md.update((byte) 0x00);
		// If it's binary, use the original buffer
		md.update(isBinary ? buffer : buffer2, 0, newSize);
		
		byte[] digest = md.digest();
		char[] digestChar = new char[digest.length * 2];
		for(int i = 0; i < digest.length; ++i) {
			digestChar[i*2] = bitsToHex[(digest[i] >> 4) & 0xF];
			digestChar[i*2 + 1] = bitsToHex[digest[i] & 0xF];
		}
		return new String(digestChar);
	}
	
	private static boolean checkGitHubForBuiltInFiles(List<BuiltInFile> files) {
		if(MCWorldExporter.offlineMode)
			return false;
		try{
			String builtin_filesTreeURL = null;
			JsonElement rootTree = Json.read(new URI("https://api.github.com/repos/" + MCWorldExporter.GitHubRepository + 
													"/git/trees/main").toURL(), false);
			if(rootTree == null)
				return false;
			if(!rootTree.isJsonObject())
				return false;
			if(!rootTree.getAsJsonObject().has("tree"))
				return false;
			for(JsonElement el : rootTree.getAsJsonObject().getAsJsonArray("tree")) {
				if(!el.isJsonObject())
					continue;
				if(!el.getAsJsonObject().has("path"))
					continue;
				if(!el.getAsJsonObject().get("path").getAsString().equalsIgnoreCase("builtin_files"))
					continue;
				if(!el.getAsJsonObject().has("url"))
					continue;
				builtin_filesTreeURL = el.getAsJsonObject().get("url").getAsString();
				break;
			}
			
			if(builtin_filesTreeURL == null)
				return false;
			
			// Now we want to recursively get the files inside of the builtin folder in GitHub.
			JsonElement builtinTree = Json.read(new URI(builtin_filesTreeURL + "?recursive=1").toURL(), false);
			if(builtinTree == null)
				return false;
			if(!builtinTree.isJsonObject())
				return false;
			if(!builtinTree.getAsJsonObject().has("tree"))
				return false;
			for(JsonElement el : builtinTree.getAsJsonObject().getAsJsonArray("tree")) {
				if(!el.isJsonObject())
					continue;
				JsonObject obj = el.getAsJsonObject();
				String type = "";
				if(obj.has("type"))
					type = obj.get("type").getAsString();
				if(!type.equals("blob"))
					continue;
				String name = null;
				if(obj.has("path"))
					name = obj.get("path").getAsString();
				else
					continue;
				String hash = null;
				if(obj.has("sha"))
					hash = obj.get("sha").getAsString();
				else
					continue;
				
				files.add(new BuiltInFile(name, 
						new URI("https://raw.githubusercontent.com/" + MCWorldExporter.GitHubRepository + 
								"/main/builtin_files/" + name).toURL(), 
						hash));
			}
			return true;
		}catch(Exception ex) {
		}
		return false;
	}
	
}
