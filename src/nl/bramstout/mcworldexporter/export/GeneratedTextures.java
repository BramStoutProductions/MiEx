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

package nl.bramstout.mcworldexporter.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.image.ImageWriter;
import nl.bramstout.mcworldexporter.world.World;

public class GeneratedTextures {
	
	public static boolean storeGeneratedTexturesInExport = false;
	public static String generatedTexturesResourcePackName = "base_resource_pack";
	
	/**
	 * Takes in a texture name and converts it into a texture resource identifier.
	 * @param name
	 * @return
	 */
	public static String getTextureId(String name) {
		name = name.replace(':', '/');
		if(storeGeneratedTexturesInExport) {
			return "./" + Exporter.chunksFolder.getName() + "/textures/" + name;
		}else {
			return "miex:" + name;
		}
	}
	
	public static File getTextureFile(String name) {
		String texId = getTextureId(name);
		if(!texId.contains("."))
			texId = texId + ".png";
		if(storeGeneratedTexturesInExport) {
			return new File(Exporter.chunksFolder.getParentFile(), texId);
		}else {
			int colonId = texId.indexOf(':');
			String namespace = texId.substring(0, colonId);
			String texName = texId.substring(colonId+1);
			return new File(FileUtil.getResourcePackDir(), generatedTexturesResourcePackName + "/assets/" + 
							namespace + "/textures/" + texName);
		}
	}
	
	public static boolean textureExists(String name) {
		return getTextureFile(name).exists();
	}
	
	/**
	 * Saves the image to disk and returns the texture id.
	 * @param name
	 * @param image
	 * @return
	 */
	public static String writeTexture(String name, BufferedImage image) {
		String texId = getTextureId(name);
		File texFile = getTextureFile(name);
		if(!texFile.getParentFile().exists())
			texFile.getParentFile().mkdirs();
		ImageWriter.writeImage(texFile, image);
		return texId;
	}
	
	/**
	 * If the texture doesn't yet exist, download it and return the texture id.
	 * @param name
	 * @param url
	 * @return
	 */
	public static String downloadTexture(String name, String url) {
		String texId = getTextureId(name);
		File texFile = getTextureFile(name);
		
		if(!texFile.exists()) {
			if(MCWorldExporter.offlineMode) {
				System.out.println("Missing online texture " + url + " > " + name);
				World.handleError(new RuntimeException("Could not download missing texture due to offline mode"));
			}else {
				try {
					if(!texFile.getParentFile().exists())
						texFile.getParentFile().mkdirs();
					
					URL url2 = new URI(url).toURL();
					Files.copy(url2.openStream(), texFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		return texId;
	}
	
}
