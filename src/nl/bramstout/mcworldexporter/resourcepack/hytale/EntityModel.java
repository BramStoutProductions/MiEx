package nl.bramstout.mcworldexporter.resourcepack.hytale;

import com.google.gson.JsonObject;

public class EntityModel {
	
	private String model;
	private String texture;
	private String gradientSet;
	private String gradientId;
	
	public EntityModel(JsonObject data) {
		model = "";
		texture = "";
		gradientSet = "";
		gradientId = "";
		
		if(data.has("Model"))
			model = data.get("Model").getAsString();
		
		if(data.has("Texture"))
			texture = data.get("Texture").getAsString();
		
		if(data.has("GradientSet"))
			gradientSet = data.get("GradientSet").getAsString();
		
		if(data.has("GradientId"))
			gradientId = data.get("GradientId").getAsString();
		
		if(model.indexOf(':') == -1 && !model.isEmpty())
			model = "hytale:" + model;
		
		if(texture.indexOf(':') == -1 && !texture.isEmpty())
			texture = "hytale:" + texture;
		int sep = texture.lastIndexOf('.');
		if(sep != -1)
			texture = texture.substring(0, sep);
	}
	
	public String getModel() {
		return model;
	}

	public String getTexture() {
		return texture;
	}

	public String getGradientSet() {
		return gradientSet;
	}

	public String getGradientId() {
		return gradientId;
	}

}
