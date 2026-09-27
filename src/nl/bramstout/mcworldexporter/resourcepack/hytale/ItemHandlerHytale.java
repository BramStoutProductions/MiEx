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
	private String parentId;
	
	public ItemHandlerHytale(JsonObject data) {
		modelId = "";
		texture = "";
		
		parentId = null;

		// Items can inherit their model and texture from a parent item.
		if(data.has("Parent") && data.get("Parent").isJsonPrimitive()) {
			parentId = data.get("Parent").getAsString();
			if(parentId.indexOf(':') == -1)
				parentId = "hytale:" + parentId;
		}
		
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
		return getModel(name, data, displayContext, 0);
	}

	private Model getModel(String name, NbtTagCompound data, String displayContext, int depth) {
		if(this.modelId.isEmpty()) {
			// No model defined on this item, so try the parent item.
			if(this.parentId != null && depth < 16) {
				ItemHandler parentHandler = ResourcePacks.getItemHandler(this.parentId, data);
				if(parentHandler instanceof ItemHandlerHytale && parentHandler != this) {
					ItemHandlerHytale parent = (ItemHandlerHytale) parentHandler;
					if(!parent.modelId.isEmpty() && !this.texture.isEmpty()) {
						// Parent's model, but with our own texture.
						ItemHandlerHytale merged = new ItemHandlerHytale(new JsonObject());
						merged.modelId = parent.modelId;
						merged.texture = this.texture;
						return merged.getModel(name, data, displayContext, depth + 1);
					}
					return parent.getModel(name, data, displayContext, depth + 1);
				}
			}
			// Nothing to export, rather than looking up a model with an empty name.
			return null;
		}
		int modelId = ModelRegistry.getIdForName(this.modelId, false);
		Model model = ModelRegistry.getModel(modelId);
		if(model == null)
			return null;
		// Make sure to make a copy of it, so that we can edit it.
		model = new Model(model);
		model.setImmoveable();
		
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
			face.uvs0U = face.uvs0U * texScaleU;
			face.uvs0V = 16f - face.uvs0V * texScaleV;
			face.uvs1U = face.uvs1U * texScaleU;
			face.uvs1V = 16f - face.uvs1V * texScaleV;
			face.uvs2U = face.uvs2U * texScaleU;
			face.uvs2V = 16f - face.uvs2V * texScaleV;
			face.uvs3U = face.uvs3U * texScaleU;
			face.uvs3V = 16f - face.uvs3V * texScaleV;
		}
		
		return model;
	}

}
