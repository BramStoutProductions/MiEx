package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import com.google.gson.JsonParser;

public class DefaultRenderControllerBedrockEdition extends RenderControllerBedrockEdition{

	public DefaultRenderControllerBedrockEdition() {
		// Most basic render controller
		super(JsonParser.parseString("{\"geometry\":\"Geometry.default\", " + 
									"\"materials\": [ { \"*\": \"Material.default\" } ], " + 
									"\"textures\": [ \"Texture.default\" ]}").getAsJsonObject());
	}

}
