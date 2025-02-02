package nl.bramstout.mcworldexporter.entity.builtins;

import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.resourcepack.EntityHandler;

public class EntityBuiltinsRegistry {

	public static Map<String, EntityHandler> builtins = new HashMap<String, EntityHandler>();
	
	static {
		builtins.put("minecraft:item_frame", new EntityHandlerItemFrame());
		builtins.put("minecraft:frame", new EntityHandlerItemFrame());
		builtins.put("minecraft:ItemFrame", new EntityHandlerItemFrame());
		builtins.put("minecraft:glow_item_frame", new EntityHandlerItemFrame());
		builtins.put("minecraft:glow_frame", new EntityHandlerItemFrame());
		builtins.put("minecraft:GlowItemFrame", new EntityHandlerItemFrame());
		builtins.put("minecraft:painting", new EntityHandlerPainting());
	}
	
}
