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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds potential locations of resource pack data.
 * Worlds can indicate resource pack sources that it has
 * and MiEx can then make sure that those resource packs
 * are actually loaded.
 */
public class ResourcePackSource {
	
	private String name;
	private List<String> sourceUuids;
	private List<File> sources;
	
	public ResourcePackSource(String name) {
		this.name = name;
		this.sourceUuids = new ArrayList<String>();
		this.sources = new ArrayList<File>();
	}
	
	public void addSource(String uuid, File file) {
		this.sourceUuids.add(uuid);
		this.sources.add(file);
	}
	
	public boolean isEmpty() {
		return sources.isEmpty();
	}
	
	public List<String> getSourceUuids(){
		return sourceUuids;
	}
	
	public List<File> getSources(){
		return sources;
	}
	
	public String getName() {
		return name;
	}
	
	private static byte[] hashBuffer = new byte[1024*1024];
	
	public static String getHash(File file) {
		if(file == null)
			return "";
		if(!file.exists())
			return "";
		
		if(file.isDirectory())
			return Long.toHexString(getDirectoryHash(file));
		else if(file.isFile())
			return Long.toHexString(getFileHash(file));
		return "";
	}
	
	public static long getDirectoryHash(File file) {
		long hash = file.getName().hashCode();
		for(File f : file.listFiles()) {
			if(f.isFile())
				hash = hash * 31 + getFileHash(f);
			else if(f.isDirectory())
				hash = hash * 31 + getDirectoryHash(f);
		}
		return hash;
	}
	
	public static long getFileHash(File file) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			
			long hash = 0;
			
			int read = 0;
			while(read >= 0) {
				read = fis.read(hashBuffer);
				if(read == -1)
					break;
				
				for(int i = 0; i < read; ++i) {
					hash = hash * 31 + hashBuffer[i];
				}
			}
			
			fis.close();
			return hash;
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		if(fis != null) {
			try {
				fis.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return 0;
	}

}
