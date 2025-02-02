package nl.bramstout.mcworldexporter.resourcepack.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockStateHandler;
import nl.bramstout.mcworldexporter.resourcepack.Tints;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;

public class BlockStateHandlerJavaEdition extends BlockStateHandler{

	private List<BlockStatePart> parts;
	
	public BlockStateHandlerJavaEdition(String name, JsonObject data) {
		this.parts = new ArrayList<BlockStatePart>();
		
		if(data == null)
			return;
		
		boolean doubleSided = Config.doubleSided.contains(name);
		
		if(data.has("variants")) {
			for(Entry<String, JsonElement> variant : data.get("variants").getAsJsonObject().entrySet()) {
				parts.add(new BlockStateVariant(variant.getKey(), variant.getValue(), doubleSided));
			}
		} else if(data.has("multipart")) {
			for(JsonElement part : data.get("multipart").getAsJsonArray().asList()) {
				parts.add(new BlockStateMultiPart(part, doubleSided));
			}
		}
		
		if(Config.noOcclusion.contains(name)) {
			for(BlockStatePart part : parts)
				part.noOcclusion();
		}
	}
	
	@Override
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, BlockState state) {
		List<List<Model>> models = new ArrayList<List<Model>>();
		BlockStatePart part = null;
		for(int i = 0; i < parts.size(); ++i) {
			part = parts.get(i);
			if(part.usePart(properties, x, y, z)) {
				models.add(part.models);
			}
		}
		Tint tint = Tints.getTint(state.getName());
		Color tintColor = null;
		if(tint != null)
			tintColor = tint.getTint(properties);
		return new BakedBlockState(state.getName(), models, state.isTransparentOcclusion(), 
				state.isLeavesOcclusion(), state.isDetailedOcclusion(), state.isIndividualBlocks(), 
				state.hasLiquid(properties), state.isCaveBlock(), state.hasRandomOffset(), 
				state.hasRandomYOffset(), state.isGrassColormap(), state.isFoliageColormap(), 
				state.isWaterColormap(), state.isDoubleSided(), state.hasRandomAnimationXZOffset(),
				state.hasRandomAnimationYOffset(), state.isLodNoUVScale(), state.getLodPriority(), tintColor, state.needsConnectionInfo());
	}

	@Override
	public String getDefaultTexture() {
		BlockStatePart part = null;
		for(int i = 0; i < parts.size(); ++i) {
			part = parts.get(i);
			String defaultTexture = part.getDefaultTexture();
			if(defaultTexture != null)
				return defaultTexture;
		}
		return "";
	}

	@Override
	public boolean needsConnectionInfo() {
		BlockStatePart part = null;
		for(int i = 0; i < parts.size(); ++i) {
			part = parts.get(i);
			if(part.needsConnectionInfo()) {
				return true;
			}
		}
		return false;
	}

}
