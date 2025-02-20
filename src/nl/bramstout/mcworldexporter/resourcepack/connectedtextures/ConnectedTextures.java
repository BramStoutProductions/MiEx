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

package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public class ConnectedTextures {
	
	public static class BlockStateConstraint{
		
		public Map<String, List<String>> checks = new HashMap<String, List<String>>();
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof BlockStateConstraint))
				return false;
			return checks.equals(((BlockStateConstraint) obj).checks);
		}
		
		@Override
		public int hashCode() {
			return checks.hashCode();
		}
		
		public boolean meetsConstraint(NbtTagCompound properties) {
			for(Entry<String, List<String>> entry : checks.entrySet()) {
				NbtTag tag = properties.get(entry.getKey());
				if(tag == null)
					return false;
				if(!entry.getValue().contains(tag.asString()))
					return false;
			}
			return true;
		}
		
	}
	
	private static Map<String, List<ConnectedTexture>> connectedTexturesByTile = new HashMap<String, List<ConnectedTexture>>();
	private static Map<String, Map<BlockStateConstraint, List<ConnectedTexture>>> connectedTexturesByBlock = 
			new HashMap<String, Map<BlockStateConstraint, List<ConnectedTexture>>>();
	
	private static ConnectedTexturesLoader[] loaders = new ConnectedTexturesLoader[] {
		new OptifineLoader()	
	};
	public static void load() {
		connectedTexturesByTile.clear();
		connectedTexturesByBlock.clear();
		for(ConnectedTexturesLoader loader : loaders)
			loader.load();
	}
	
	protected static void registerConnectedTextureByTile(String tile, ConnectedTexture connectedTexture) {
		List<ConnectedTexture> connectedTextures = connectedTexturesByTile.get(tile);
		if(connectedTextures == null) {
			connectedTextures = new ArrayList<ConnectedTexture>();
			connectedTexturesByTile.put(tile, connectedTextures);
		}
		connectedTextures.add(connectedTexture);
	}
	
	protected static void registerConnectedTextureByBlock(String block, BlockStateConstraint constraint, ConnectedTexture connectedTexture) {
		Map<BlockStateConstraint, List<ConnectedTexture>> connectedTextures = connectedTexturesByBlock.get(block);
		if(connectedTextures == null) {
			connectedTextures = new HashMap<BlockStateConstraint, List<ConnectedTexture>>();
			connectedTexturesByBlock.put(block, connectedTextures);
		}
		List<ConnectedTexture> connectedTextures2 = connectedTextures.getOrDefault(constraint, null);
		if(connectedTextures2 == null) {
			connectedTextures2 = new ArrayList<ConnectedTexture>();
			connectedTextures.put(constraint, connectedTextures2);
		}
		connectedTextures2.add(connectedTexture);
	}
	
	public static Entry<ConnectedTexture, List<ConnectedTexture>> getConnectedTexture(String block, NbtTagCompound properties, String texture) {
		List<ConnectedTexture> connectedTextures = getConnectedTextures(block, properties, texture);
		if(connectedTextures == null)
			return null;
		ConnectedTexture main = null;
		List<ConnectedTexture> overlays = null;
		for(int i = 0; i < connectedTextures.size(); ++i) {
			if(connectedTextures.get(i).isOverlay()) {
				if(overlays == null)
					overlays = new ArrayList<ConnectedTexture>();
				overlays.add(connectedTextures.get(i));
			}
			if(!connectedTextures.get(i).isOverlay() && main == null)
				main = connectedTextures.get(i);
		}
		final ConnectedTexture fMain = main;
		final List<ConnectedTexture> fOverlays = overlays;
		return new Entry<ConnectedTexture, List<ConnectedTexture>>(){
			
			@Override
			public ConnectedTexture getKey() {
				return fMain;
			}

			@Override
			public List<ConnectedTexture> getValue() {
				return fOverlays;
			}

			@Override
			public List<ConnectedTexture> setValue(List<ConnectedTexture> value) {
				return null;
			}
			
		};
	}
	
	private static List<ConnectedTexture> getConnectedTextures(String block, NbtTagCompound properties, String texture) {
		Map<BlockStateConstraint, List<ConnectedTexture>> byBlock = connectedTexturesByBlock.get(block);
		List<ConnectedTexture> byTile = connectedTexturesByTile.get(texture);
		
		if(byBlock == null && byTile == null)
			return null;
		
		List<ConnectedTexture> res = new ArrayList<ConnectedTexture>();
		int maxPriority = 0;
		if(byBlock != null) {
			for(Entry<BlockStateConstraint, List<ConnectedTexture>> texs : byBlock.entrySet()) {
				if(!texs.getKey().meetsConstraint(properties))
					continue;
				for(ConnectedTexture tex : texs.getValue()) {
					if(tex.getPriority() == maxPriority)
						res.add(tex);
					else if(tex.getPriority() > maxPriority) {
						maxPriority = tex.getPriority();
						res.clear();
						res.add(tex);
					}
				}
			}
		}
		if(byTile != null) {
			for(ConnectedTexture tex : byTile) {
				if(tex.getPriority() == maxPriority)
					res.add(tex);
				else if(tex.getPriority() > maxPriority) {
					maxPriority = tex.getPriority();
					res.clear();
					res.add(tex);
				}
			}
		}
		if(res.size() <= 0)
			return null;
		if(res.size() == 1)
			return res;
		// Sort based on names
		res.sort(new Comparator<ConnectedTexture>() {

			@Override
			public int compare(ConnectedTexture o1, ConnectedTexture o2) {
				String s1 = o1.getName();
				String s2 = o2.getName();
				for(int i = 0; i < Math.min(s1.length(), s2.length()); ++i) {
					int diff = s2.codePointAt(i) - s1.codePointAt(i);
					if(diff == 0)
						continue;
					return diff;
				}
				return 0;
			}
			
		});
		return res;
	}
	
}
