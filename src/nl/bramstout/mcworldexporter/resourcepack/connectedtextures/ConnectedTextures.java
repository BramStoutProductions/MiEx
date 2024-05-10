package nl.bramstout.mcworldexporter.resourcepack.connectedtextures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectedTextures {
	
	private static Map<String, List<ConnectedTexture>> connectedTexturesByTile = new HashMap<String, List<ConnectedTexture>>();
	private static Map<String, List<ConnectedTexture>> connectedTexturesByBlock = new HashMap<String, List<ConnectedTexture>>();
	
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
	
	protected static void registerConnectedTextureByBlock(String block, ConnectedTexture connectedTexture) {
		List<ConnectedTexture> connectedTextures = connectedTexturesByBlock.get(block);
		if(connectedTextures == null) {
			connectedTextures = new ArrayList<ConnectedTexture>();
			connectedTexturesByBlock.put(block, connectedTextures);
		}
		connectedTextures.add(connectedTexture);
	}
	
	public static ConnectedTexture getConnectedTexture(String block, String texture) {
		List<ConnectedTexture> byBlock = connectedTexturesByBlock.get(block);
		List<ConnectedTexture> byTile = connectedTexturesByTile.get(texture);
		
		if(byBlock == null && byTile == null)
			return null;
		
		List<ConnectedTexture> res = new ArrayList<ConnectedTexture>();
		int maxPriority = 0;
		if(byBlock != null) {
			for(ConnectedTexture tex : byBlock) {
				if(tex.getPriority() == maxPriority)
					res.add(tex);
				else if(tex.getPriority() > maxPriority) {
					maxPriority = tex.getPriority();
					res.clear();
					res.add(tex);
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
			return res.get(0);
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
		return res.get(0);
	}
	
}
