package nl.bramstout.mcworldexporter.translation;

import nl.bramstout.mcworldexporter.MCWorldExporter;

public class TranslationRegistry {
	
	public static BlockTranslation BLOCK_BEDROCK = new BlockTranslation("bedrock");
	public static BiomeTranslation BIOME_BEDROCK = new BiomeTranslation("bedrock");
	public static FilePathMapping FILE_PATH_MAPPING_BEDROCK = new FilePathMapping("bedrock");
	
	public static BiomeTranslation BIOME_JAVA = new BiomeTranslation("java");
	public static BlockTranslation BLOCK_JAVA = new BlockTranslation("java");
	
	public static void load() {
		BLOCK_BEDROCK.load();
		BIOME_BEDROCK.load();
		FILE_PATH_MAPPING_BEDROCK.load();
		
		BIOME_JAVA.load();
		BLOCK_JAVA.load();
		
		if(MCWorldExporter.getApp() != null)
			if(MCWorldExporter.getApp().getWorld() != null)
				if(MCWorldExporter.getApp().getWorld().getBlockConnectionsTranslation() != null)
					MCWorldExporter.getApp().getWorld().getBlockConnectionsTranslation().load();
	}
	
	
}
