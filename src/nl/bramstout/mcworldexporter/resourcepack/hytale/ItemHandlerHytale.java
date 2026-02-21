package nl.bramstout.mcworldexporter.resourcepack.hytale;

import java.io.File;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.ItemHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class ItemHandlerHytale extends ItemHandler{

	private String modelId;
	private String texture;
	
	public ItemHandlerHytale(JsonObject data) {
		modelId = "";
		texture = "";
		
		if(data.has("Model"))
			modelId = data.get("Model").getAsString();
		
		if(data.has("Texture"))
			texture = data.get("Texture").getAsString();
		
		if(modelId.indexOf(':') == -1 && !modelId.isEmpty())
			modelId = "hytale:" + modelId;
		
		if(texture.indexOf(':') == -1 && !texture.isEmpty())
			texture = "hytale:" + texture;
		int sep = texture.lastIndexOf('.');
		if(sep != -1)
			texture = texture.substring(0, sep);
	}
	
	@Override
	public Model getModel(String name, NbtTagCompound data, String displayContext) {
		int modelId = ModelRegistry.getIdForName(this.modelId, false);
		Model model = ModelRegistry.getModel(modelId);
		if(model == null)
			return null;
		// Make sure to make a copy of it, so that we can edit it.
		model = new Model(model);
		
		float textureWidth = 32f;
		float textureHeight = 32f;
		
		File textureFile = ResourcePacks.getTexture(texture);
		if(textureFile != null) {
			long textureSize = FileUtil.getImageSize(textureFile);
			textureWidth = (float) (textureSize >> 32);
			textureHeight = (float) (textureSize & 0xFFFFFFFFL);
		}
		float texScaleU = 16f / textureWidth;
		float texScaleV = 16f / textureHeight;
		
		model.addTexture("#north", texture);
		model.addTexture("#south", texture);
		model.addTexture("#west", texture);
		model.addTexture("#east", texture);
		model.addTexture("#up", texture);
		model.addTexture("#down", texture);
		
		for(ModelFace face : model.getFaces()) {
			// UVs for custom models are based on the texture resolution,
			// but since we don't know what the texture is when loading in
			// the model, we needed to defer it to now. So, now we can update
			// the UVs to take into account the texture resolution.
			face.getUVs()[0] = face.getUVs()[0] * texScaleU;
			face.getUVs()[1] = 16f - face.getUVs()[1] * texScaleV;
			face.getUVs()[2] = face.getUVs()[2] * texScaleU;
			face.getUVs()[3] = 16f - face.getUVs()[3] * texScaleV;
			face.getUVs()[4] = face.getUVs()[4] * texScaleU;
			face.getUVs()[5] = 16f - face.getUVs()[5] * texScaleV;
			face.getUVs()[6] = face.getUVs()[6] * texScaleU;
			face.getUVs()[7] = 16f - face.getUVs()[7] * texScaleV;
		}
		
		return model;
	}

}
