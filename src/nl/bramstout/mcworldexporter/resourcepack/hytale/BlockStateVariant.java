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

package nl.bramstout.mcworldexporter.resourcepack.hytale;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.Color;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.model.BakedBlockState;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.model.ModelFace;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.resourcepack.BlockAnimationHandler;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.resourcepack.Tints.Tint;
import nl.bramstout.mcworldexporter.resourcepack.Tints.TintLayers;

public class BlockStateVariant {

	public static class CubeTextures{
		public String north;
		public String south;
		public String east;
		public String west;
		public String up;
		public String down;
		public float weight;
		
		public CubeTextures(JsonObject data) {
			if(data.has("All")) {
				String tex = "hytale:" + data.get("All").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.north = tex;
				this.south = tex;
				this.east = tex;
				this.west = tex;
				this.up = tex;
				this.down = tex;
			}
			if(data.has("Sides")) {
				String tex = "hytale:" + data.get("Sides").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.north = tex;
				this.south = tex;
				this.east = tex;
				this.west = tex;
			}
			if(data.has("UpDown")) {
				String tex = "hytale:" + data.get("UpDown").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.up = tex;
				this.down = tex;
			}
			if(data.has("North")) {
				String tex = "hytale:" + data.get("North").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.north = tex;
			}
			if(data.has("South")) {
				String tex = "hytale:" + data.get("South").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.south = tex;
			}
			if(data.has("East")) {
				String tex = "hytale:" + data.get("East").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.east = tex;
			}
			if(data.has("West")) {
				String tex = "hytale:" + data.get("West").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.west = tex;
			}
			if(data.has("Up")) {
				String tex = "hytale:" + data.get("Up").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.up = tex;
			}
			if(data.has("Down")) {
				String tex = "hytale:" + data.get("Down").getAsString();
				int sep = tex.lastIndexOf('.');
				if(sep != -1)
					tex = tex.substring(0, sep);
				this.down = tex;
			}
			this.weight = 1f;
			if(data.has("Weight"))
				this.weight = data.get("Weight").getAsFloat();
		}
		
		public CubeTextures(CubeTextures other) {
			this.north = other.north;
			this.south = other.south;
			this.east = other.east;
			this.west = other.west;
			this.up = other.up;
			this.down = other.down;
			this.weight = other.weight;
		}
	}
	
	private static class ModelTexture{
		
		public String texture;
		public float weight;
		
		public ModelTexture(JsonObject data) {
			this.texture = "";
			this.weight = 1f;
			
			if(data.has("Texture")) {
				this.texture = "hytale:" + data.get("Texture").getAsString();
				int sep = this.texture.lastIndexOf('.');
				if(sep != -1)
					this.texture = this.texture.substring(0, sep);
			}
			
			if(data.has("Weight"))
				this.weight = data.get("Weight").getAsFloat();
		}
		
		public ModelTexture(ModelTexture other) {
			this.texture = other.texture;
			this.weight = other.weight;
		}
		
	}
	
	private static enum RandomRotation{
		
		None("None"),
		YawPitchRollStep1("YawPitchRollStep1"),
		YawStep1("YawStep1"),
		YawStep1XZ("YawStep1XZ"),
		YawStep90("YawStep90");
		
		private String id;
		
		RandomRotation(String id){
			this.id = id;
		}
		
	}
	
	@SuppressWarnings("unused")
	private String name;
	private String drawType;
	private String opacity;
	private List<CubeTextures> cubeTextures;
	private float cubeTexturesTotalWeight;
	private String cubeTextureSideMask;
	private String customModelId;
	private List<ModelTexture> customModelTextures;
	private float customModelTexturesTotalWeight;
	private float customModelScale;
	private RandomRotation randomRotation;
	private Color[] tintsUp;
	private Color[] tintsDown;
	private Color[] tintsNorth;
	private Color[] tintsSouth;
	private Color[] tintsWest;
	private Color[] tintsEast;
	private int biomeTintUp;
	private int biomeTintDown;
	private int biomeTintNorth;
	private int biomeTintSouth;
	private int biomeTintWest;
	private int biomeTintEast;
	private String transitionTexture;
	private String[] transitionToGroups;
	private BlockAnimationHandler animationHandler;
	
	public BlockStateVariant(String name, JsonObject data) {
		this.name = name;
		this.drawType = "";
		this.opacity = "Solid";
		this.cubeTextures = new ArrayList<CubeTextures>();
		this.cubeTexturesTotalWeight = 0f;
		this.cubeTextureSideMask = null;
		this.customModelId = null;
		this.customModelTextures = new ArrayList<ModelTexture>();
		this.customModelTexturesTotalWeight = 0f;
		this.customModelScale = 1f;
		this.randomRotation = RandomRotation.None;
		this.tintsUp = null;
		this.tintsDown = null;
		this.tintsNorth = null;
		this.tintsSouth = null;
		this.tintsWest = null;
		this.tintsEast = null;
		this.biomeTintUp = -1;
		this.biomeTintDown = -1;
		this.biomeTintNorth = -1;
		this.biomeTintSouth = -1;
		this.biomeTintWest = -1;
		this.biomeTintEast = -1;
		this.transitionTexture = null;
		this.transitionToGroups = null;
		this.animationHandler = null;
		
		this.load(data);
	}
	
	public BlockStateVariant(String name, BlockStateVariant other) {
		this.name = name;
		this.drawType = other.drawType;
		this.opacity = other.opacity;
		this.cubeTextures = new ArrayList<CubeTextures>();
		for(CubeTextures tex : other.cubeTextures)
			this.cubeTextures.add(new CubeTextures(tex));
		this.cubeTexturesTotalWeight = other.cubeTexturesTotalWeight;
		this.cubeTextureSideMask = other.cubeTextureSideMask;
		this.customModelId = other.customModelId;
		this.customModelTextures = new ArrayList<ModelTexture>();
		for(ModelTexture tex : other.customModelTextures)
			this.customModelTextures.add(new ModelTexture(tex));
		this.customModelTexturesTotalWeight = other.customModelTexturesTotalWeight;
		this.customModelScale = other.customModelScale;
		this.randomRotation = other.randomRotation;
		this.tintsUp = other.tintsUp;
		this.tintsDown = other.tintsDown;
		this.tintsNorth = other.tintsNorth;
		this.tintsSouth = other.tintsSouth;
		this.tintsWest = other.tintsWest;
		this.tintsEast = other.tintsEast;
		this.biomeTintUp = other.biomeTintUp;
		this.biomeTintDown = other.biomeTintDown;
		this.biomeTintNorth = other.biomeTintNorth;
		this.biomeTintSouth = other.biomeTintSouth;
		this.biomeTintEast = other.biomeTintEast;
		this.biomeTintWest = other.biomeTintWest;
		this.transitionTexture = other.transitionTexture;
		this.transitionToGroups = null;
		if(other.transitionToGroups != null)
			this.transitionToGroups = Arrays.copyOf(other.transitionToGroups, other.transitionToGroups.length);
		this.animationHandler = other.animationHandler;
	}
	
	public void load(JsonObject data) {
		if(data == null)
			return;
			
		if(data.has("DrawType"))
			this.drawType = data.get("DrawType").getAsString();

		if(data.has("Opacity"))
			this.opacity = data.get("Opacity").getAsString();
		
		if(data.has("Textures")) {
			this.cubeTextures.clear();
			this.cubeTexturesTotalWeight = 0f;
			JsonArray texturesArray = data.getAsJsonArray("Textures");
			for(JsonElement el : texturesArray.asList()) {
				if(el.isJsonObject()) {
					CubeTextures tex = new CubeTextures(el.getAsJsonObject());
					this.cubeTextures.add(tex);
					this.cubeTexturesTotalWeight += tex.weight;
				}
			}
		}
		
		if(data.has("TextureSideMask")) {
			this.cubeTextureSideMask = "hytale:" + data.get("TextureSideMask").getAsString();
			int sep = this.cubeTextureSideMask.lastIndexOf('.');
			if(sep != -1)
				this.cubeTextureSideMask = this.cubeTextureSideMask.substring(0, sep);
		}
		
		if(data.has("CustomModel")) {
			this.customModelId = data.get("CustomModel").getAsString();
			if(this.customModelId.indexOf(':') == -1)
				this.customModelId = "hytale:" + this.customModelId;
			//String customModelId = "hytale:" + data.get("CustomModel").getAsString();
			//int modelId = ModelRegistry.getIdForName(customModelId, false);
			//this.customModel = ModelRegistry.getModel(modelId);
		}
		
		if(data.has("CustomModelTexture")){
			this.customModelTextures.clear();
			this.customModelTexturesTotalWeight = 0f;
			JsonArray texturesArray = data.getAsJsonArray("CustomModelTexture");
			for(JsonElement el : texturesArray.asList()) {
				if(el.isJsonObject()) {
					ModelTexture tex = new ModelTexture(el.getAsJsonObject());
					this.customModelTextures.add(tex);
					this.customModelTexturesTotalWeight += tex.weight;
				}
			}
		}
		
		if(data.has("CustomModelScale"))
			this.customModelScale = data.get("CustomModelScale").getAsFloat();
		
		if(data.has("CustomModelAnimation") && data.has("Looping") && data.get("Looping").getAsBoolean()) {
			// We only want looping animations. It wouldn't make much sense to export out single fire animations.
			String animationId = data.get("CustomModelAnimation").getAsString();
			int sep = animationId.lastIndexOf('.');
			if(sep != -1)
				animationId = animationId.substring(0, sep);
			if(animationId.indexOf(':') == -1)
				animationId = "hytale:" + animationId;
			
			this.animationHandler = ResourcePacks.getBlockAnimationHandler(animationId);
		}
		
		if(data.has("RandomRotation")) {
			String val = data.get("RandomRotation").getAsString();
			for(RandomRotation rot : RandomRotation.values()) {
				if(rot.id.equals(val)) {
					this.randomRotation = rot;
					break;
				}
			}
		}
		
		if(data.has("TransitionTexture")) {
			this.transitionTexture = data.get("TransitionTexture").getAsString();
			if(this.transitionTexture.indexOf(':') == -1)
				this.transitionTexture = "hytale:" + this.transitionTexture;
			int sep = this.transitionTexture.lastIndexOf('.');
			if(sep != -1)
				this.transitionTexture = this.transitionTexture.substring(0, sep);
		}
		
		if(data.has("TransitionToGroups")) {
			JsonArray groupsArray = data.getAsJsonArray("TransitionToGroups");
			if(!groupsArray.isEmpty()) {
				this.transitionToGroups = new String[groupsArray.size()];
				for(int i = 0; i < groupsArray.size(); ++i) {
					String group = groupsArray.get(i).getAsString();
					if(group.indexOf(':') == -1)
						group = "hytale:" + group;
					this.transitionToGroups[i] = group;
				}
			}
		}
		
		if(data.has("Tint")) {
			JsonArray tints = data.getAsJsonArray("Tint");
			if(!tints.isEmpty()) {
				this.tintsUp = new Color[tints.size()];
				for(int i = 0; i < tints.size(); ++i) {
					int color = 0xFFFFFF;
					try {
						String val = tints.get(i).getAsString();
						if(val.charAt(0) == '#')
							val = val.substring(1);
						color = Integer.parseInt(val, 16);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					this.tintsUp[i] = new Color(color);
				}
				this.tintsDown = this.tintsUp;
				this.tintsNorth = this.tintsUp;
				this.tintsSouth = this.tintsUp;
				this.tintsEast = this.tintsUp;
				this.tintsWest = this.tintsUp;
			}
		}
		
		if(data.has("TintUp")) {
			JsonArray tints = data.getAsJsonArray("TintUp");
			if(!tints.isEmpty()) {
				this.tintsUp = new Color[tints.size()];
				for(int i = 0; i < tints.size(); ++i) {
					int color = 0xFFFFFF;
					try {
						String val = tints.get(i).getAsString();
						if(val.charAt(0) == '#')
							val = val.substring(1);
						color = Integer.parseInt(val, 16);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					this.tintsUp[i] = new Color(color);
				}
			}else {
				this.tintsUp = null;
			}
		}
		if(data.has("TintDown")) {
			JsonArray tints = data.getAsJsonArray("TintDown");
			if(!tints.isEmpty()) {
				this.tintsDown = new Color[tints.size()];
				for(int i = 0; i < tints.size(); ++i) {
					int color = 0xFFFFFF;
					try {
						String val = tints.get(i).getAsString();
						if(val.charAt(0) == '#')
							val = val.substring(1);
						color = Integer.parseInt(val, 16);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					this.tintsDown[i] = new Color(color);
				}
			}else {
				this.tintsDown = null;
			}
		}
		if(data.has("TintNorth")) {
			JsonArray tints = data.getAsJsonArray("TintNorth");
			if(!tints.isEmpty()) {
				this.tintsNorth = new Color[tints.size()];
				for(int i = 0; i < tints.size(); ++i) {
					int color = 0xFFFFFF;
					try {
						String val = tints.get(i).getAsString();
						if(val.charAt(0) == '#')
							val = val.substring(1);
						color = Integer.parseInt(val, 16);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					this.tintsNorth[i] = new Color(color);
				}
			}else {
				this.tintsNorth = null;
			}
		}
		if(data.has("TintSouth")) {
			JsonArray tints = data.getAsJsonArray("TintSouth");
			if(!tints.isEmpty()) {
				this.tintsSouth = new Color[tints.size()];
				for(int i = 0; i < tints.size(); ++i) {
					int color = 0xFFFFFF;
					try {
						String val = tints.get(i).getAsString();
						if(val.charAt(0) == '#')
							val = val.substring(1);
						color = Integer.parseInt(val, 16);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					this.tintsSouth[i] = new Color(color);
				}
			}else {
				this.tintsSouth = null;
			}
		}
		if(data.has("TintEast")) {
			JsonArray tints = data.getAsJsonArray("TintEast");
			if(!tints.isEmpty()) {
				this.tintsEast = new Color[tints.size()];
				for(int i = 0; i < tints.size(); ++i) {
					int color = 0xFFFFFF;
					try {
						String val = tints.get(i).getAsString();
						if(val.charAt(0) == '#')
							val = val.substring(1);
						color = Integer.parseInt(val, 16);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					this.tintsEast[i] = new Color(color);
				}
			}else {
				this.tintsEast = null;
			}
		}
		if(data.has("TintWest")) {
			JsonArray tints = data.getAsJsonArray("TintWest");
			if(!tints.isEmpty()) {
				this.tintsWest = new Color[tints.size()];
				for(int i = 0; i < tints.size(); ++i) {
					int color = 0xFFFFFF;
					try {
						String val = tints.get(i).getAsString();
						if(val.charAt(0) == '#')
							val = val.substring(1);
						color = Integer.parseInt(val, 16);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
					this.tintsWest[i] = new Color(color);
				}
			}else {
				this.tintsWest = null;
			}
		}
		
		if(data.has("BiomeTint")) {
			int biomeTint = data.get("BiomeTint").getAsInt();
			this.biomeTintUp = biomeTint;
			this.biomeTintDown = biomeTint;
			this.biomeTintNorth = biomeTint;
			this.biomeTintSouth = biomeTint;
			this.biomeTintEast = biomeTint;
			this.biomeTintWest = biomeTint;
		}
		
		if(data.has("BiomeTintUp"))
			this.biomeTintUp = data.get("BiomeTintUp").getAsInt();

		if(data.has("BiomeTintDown"))
			this.biomeTintDown = data.get("BiomeTintDown").getAsInt();

		if(data.has("BiomeTintNorth"))
			this.biomeTintNorth = data.get("BiomeTintNorth").getAsInt();

		if(data.has("BiomeTintSouth"))
			this.biomeTintSouth = data.get("BiomeTintSouth").getAsInt();

		if(data.has("BiomeTintEast"))
			this.biomeTintEast = data.get("BiomeTintEast").getAsInt();

		if(data.has("BiomeTintWest"))
			this.biomeTintWest = data.get("BiomeTintWest").getAsInt();
		
		// In Hytale, a tint of 0 means no tint,
		// but in MiEx, a tint of -1 means to tint, so convert.
		if(this.biomeTintUp == 0)
			this.biomeTintUp = -1;
		if(this.biomeTintDown == 0)
			this.biomeTintDown = -1;
		if(this.biomeTintNorth == 0)
			this.biomeTintNorth = -1;
		if(this.biomeTintSouth == 0)
			this.biomeTintSouth = -1;
		if(this.biomeTintEast == 0)
			this.biomeTintEast = -1;
		if(this.biomeTintWest == 0)
			this.biomeTintWest = -1;
	}
	
	public boolean hasBiomeTint() {
		return this.biomeTintNorth >= 0 || 
				this.biomeTintSouth >= 0 ||
				this.biomeTintEast >= 0 ||
				this.biomeTintWest >= 0 ||
				this.biomeTintDown >= 0 ||
				this.biomeTintUp >= 0;
	}
	
	public boolean isTransparent() {
		return !this.opacity.equals("Solid") && !this.opacity.isEmpty();
	}
	
	public boolean needsConnectionInfo() {
		return this.randomRotation == RandomRotation.YawPitchRollStep1;
	}
	
	public String getTransitionTexture() {
		return transitionTexture;
	}
	
	public String[] getTransitionToGroups() {
		return transitionToGroups;
	}
	
	public int getBiomeTintUp() {
		return biomeTintUp;
	}
	
	public Color[] getTintUp() {
		return tintsUp;
	}
	
	public String getDefaultTexture() {
		if(this.cubeTextures.size() > 0) {
			String up = this.cubeTextures.get(0).up;
			if(up != null)
				return up;
		}
		if(this.customModelTextures.size() > 0)
			return this.customModelTextures.get(0).texture;
		return "";
	}
	
	public BakedBlockState getBakedBlockState(NbtTagCompound properties, int x, int y, int z, 
										BlockState state, BlockAnimationHandler animationHandler, float frame) {
		List<List<Model>> models = new ArrayList<List<Model>>();
		List<Model> modelsCube = new ArrayList<Model>();
		List<Model> modelsCustom = new ArrayList<Model>();
		if(this.drawType.equals("Cube") || this.drawType.equals("CubeWithModel")) {
			models.add(modelsCube);
		}
		if((this.drawType.equals("Model") || this.drawType.equals("CubeWithModel")) && customModelId != null) {
			models.add(modelsCustom);
		}
		
		int numPermutations = getNumPermutations();
		
		for(int permutation = 0; permutation < numPermutations; ++permutation) {
			Matrix transform = getTransform(properties, x, y, z, permutation);
			
			if(this.drawType.equals("Cube") || this.drawType.equals("CubeWithModel")) {
				Model model = createCubeModel(x, y, z, permutation);
				if(transform != null)
					model.transform(transform);
				model.calculateOcclusions();
				modelsCube.add(model);
			}
			if((this.drawType.equals("Model") || this.drawType.equals("CubeWithModel")) && customModelId != null) {
				Model model = createCustomModel(x, y, z, permutation, animationHandler, frame);
				if(transform != null)
					model.transform(transform);
				model.calculateOcclusions();
				if(this.drawType.equals("CubeWithModel"))
					model.allowOcclusionIfFullyOccluded();
				modelsCustom.add(model);
			}
		}
		
		Tint tint = state.getTint();
		TintLayers tintColor = null;
		if(tint != null)
			tintColor = tint.getTint(properties);
		return new BakedBlockState(state.getName(), models, state.isTransparentOcclusion(), 
				state.isLeavesOcclusion(), state.isDetailedOcclusion(), state.isIndividualBlocks(), 
				state.hasLiquid(properties), state.getLiquidName(properties), state.isCaveBlock(), state.hasRandomOffset(), 
				state.hasRandomYOffset(), state.isDoubleSided(), state.hasRandomAnimationXZOffset(),
				state.hasRandomAnimationYOffset(), state.isLodNoUVScale(), state.isLodNoScale(), state.getLodPriority(), 
				tintColor, state.needsConnectionInfo(), animationHandler == null ? this.animationHandler : animationHandler);
	}
	
	private Model createCubeModel(int x, int y, int z, int permutation) {
		Model model = new Model("", null, false);
		
		float u0 = 0;
		float v0 = 0;
		float uvWidth = 16;
		float uvHeight = 16;
		float[] uvs = new float[] {
				u0, v0,
				u0 + uvWidth, v0 + uvHeight
		};
		
		// North
		float[] points = new float[] {
				 0f,  0f,  0f,
				 16f, 16f, 16f
		};
		ModelFace faceNorth = model.addFace(points, uvs, Direction.NORTH, "#north", biomeTintNorth);
		if(this.tintsNorth != null && this.tintsNorth.length > 0 && faceNorth.getTintIndex() < 0) {
			faceNorth.setFaceColour(this.tintsNorth[0].getR(), this.tintsNorth[0].getG(), this.tintsNorth[0].getB());
		}
		
		// South
		ModelFace faceSouth = model.addFace(points, uvs, Direction.SOUTH, "#south", biomeTintSouth);
		if(this.tintsSouth != null && this.tintsSouth.length > 0 && faceSouth.getTintIndex() < 0) {
			faceSouth.setFaceColour(this.tintsSouth[0].getR(), this.tintsSouth[0].getG(), this.tintsSouth[0].getB());
		}
		
		// West
		ModelFace faceWest = model.addFace(points, uvs, Direction.WEST, "#west", biomeTintWest);
		if(this.tintsWest != null && this.tintsWest.length > 0 && faceWest.getTintIndex() < 0) {
			faceWest.setFaceColour(this.tintsWest[0].getR(), this.tintsWest[0].getG(), this.tintsWest[0].getB());
		}
		
		// East
		ModelFace faceEast = model.addFace(points, uvs, Direction.EAST, "#east", biomeTintEast);
		if(this.tintsEast != null && this.tintsEast.length > 0 && faceEast.getTintIndex() < 0) {
			faceEast.setFaceColour(this.tintsEast[0].getR(), this.tintsEast[0].getG(), this.tintsEast[0].getB());
		}
		
		// Up
		ModelFace faceUp = model.addFace(points, uvs, Direction.UP, "#up", biomeTintUp);
		if(this.tintsUp != null && this.tintsUp.length > 0 && faceUp.getTintIndex() < 0) {
			faceUp.setFaceColour(this.tintsUp[0].getR(), this.tintsUp[0].getG(), this.tintsUp[0].getB());
		}

		// Down
		ModelFace faceDown = model.addFace(points, uvs, Direction.DOWN, "#down", biomeTintDown);
		if(this.tintsDown != null && this.tintsDown.length > 0 && faceDown.getTintIndex() < 0) {
			faceDown.setFaceColour(this.tintsDown[0].getR(), this.tintsDown[0].getG(), this.tintsDown[0].getB());
		}
		
		if(this.cubeTextureSideMask != null && !this.cubeTextureSideMask.isEmpty()) {
			points = new float[] {
					 -0.01f,  0f,  -0.01f,
					 16.01f, 16f, 16.01f
			};
			
			ModelFace faceNorth2 = model.addFace(points, uvs, Direction.NORTH, "#sideMask", 100);
			if(this.tintsNorth != null && this.tintsNorth.length > 0 && faceNorth.getTintIndex() < 0) {
				faceNorth2.setFaceColour(this.tintsNorth[0].getR(), this.tintsNorth[0].getG(), this.tintsNorth[0].getB());
			}
			
			// South
			ModelFace faceSouth2 = model.addFace(points, uvs, Direction.SOUTH, "#sideMask", 100);
			if(this.tintsSouth != null && this.tintsSouth.length > 0 && faceSouth.getTintIndex() < 0) {
				faceSouth2.setFaceColour(this.tintsSouth[0].getR(), this.tintsSouth[0].getG(), this.tintsSouth[0].getB());
			}
			
			// West
			ModelFace faceWest2 = model.addFace(points, uvs, Direction.WEST, "#sideMask", 100);
			if(this.tintsWest != null && this.tintsWest.length > 0 && faceWest.getTintIndex() < 0) {
				faceWest2.setFaceColour(this.tintsWest[0].getR(), this.tintsWest[0].getG(), this.tintsWest[0].getB());
			}
			
			// East
			ModelFace faceEast2 = model.addFace(points, uvs, Direction.EAST, "#sideMask", 100);
			if(this.tintsEast != null && this.tintsEast.length > 0 && faceEast.getTintIndex() < 0) {
				faceEast2.setFaceColour(this.tintsEast[0].getR(), this.tintsEast[0].getG(), this.tintsEast[0].getB());
			}
		}
		
		
		CubeTextures textures = getCubeTextureForPermutation(permutation);
		if(textures == null && this.cubeTextures.size() > 0)
			textures = this.cubeTextures.get(0);
		
		if(textures != null) {
			model.setWeight(textures.weight);
			model.addTexture("#north", textures.north);
			model.addTexture("#south", textures.south);
			model.addTexture("#west", textures.west);
			model.addTexture("#east", textures.east);
			model.addTexture("#up", textures.up);
			model.addTexture("#down", textures.down);
		}
		if(this.cubeTextureSideMask != null && !this.cubeTextureSideMask.isEmpty()) {
			model.addTexture("#sideMask", cubeTextureSideMask);
		}
		
		return model;
	}
	
	private Model createCustomModel(int x, int y, int z, int permutation, BlockAnimationHandler animationHandler, float frame) {
		int customModelId = ModelRegistry.getIdForName(this.customModelId, false);
		Model customModel = ModelRegistry.getModel(customModelId);
		Model model = null;
		if(animationHandler != null)
			model = customModel.getAnimatedVersion(animationHandler, frame);
		else
			model = new Model(customModel);
		
		ModelTexture textures = getModelTextureForPermutation(permutation);
		if(textures == null && this.customModelTextures.size() > 0)
			textures = this.customModelTextures.get(0);
		if(textures == null)
			return model;
		
		model.setWeight(textures.weight);
		
		String textureId = textures.texture;
		float textureWidth = 32f;
		float textureHeight = 32f;
		
		File textureFile = ResourcePacks.getTexture(textureId);
		if(textureFile != null) {
			long textureSize = FileUtil.getImageSize(textureFile);
			textureWidth = (float) (textureSize >> 32);
			textureHeight = (float) (textureSize & 0xFFFFFFFFL);
		}
		float texScaleU = 16f / textureWidth;
		float texScaleV = 16f / textureHeight;
		
		if(textures != null) {
			model.addTexture("#north", textureId);
			model.addTexture("#south", textureId);
			model.addTexture("#west", textureId);
			model.addTexture("#east", textureId);
			model.addTexture("#up", textureId);
			model.addTexture("#down", textureId);
		}
		
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
			
			
			if(face.getTexture().equals("#north")) {
				face.setTintIndex(biomeTintNorth);
				
				if(this.tintsNorth != null && this.tintsNorth.length > 0 && face.getTintIndex() < 0)
					face.setFaceColour(this.tintsNorth[0].getR(), this.tintsNorth[0].getG(), this.tintsNorth[0].getB());
			}else if(face.getTexture().equals("#south")) {
				face.setTintIndex(biomeTintSouth);
				
				if(this.tintsSouth != null && this.tintsSouth.length > 0 && face.getTintIndex() < 0)
					face.setFaceColour(this.tintsSouth[0].getR(), this.tintsSouth[0].getG(), this.tintsSouth[0].getB());
			}else if(face.getTexture().equals("#east")) {
				face.setTintIndex(biomeTintEast);
				
				if(this.tintsEast != null && this.tintsEast.length > 0 && face.getTintIndex() < 0)
					face.setFaceColour(this.tintsEast[0].getR(), this.tintsEast[0].getG(), this.tintsEast[0].getB());
			}else if(face.getTexture().equals("#west")) {
				face.setTintIndex(biomeTintWest);
				
				if(this.tintsWest != null && this.tintsWest.length > 0 && face.getTintIndex() < 0)
					face.setFaceColour(this.tintsWest[0].getR(), this.tintsWest[0].getG(), this.tintsWest[0].getB());
			}else if(face.getTexture().equals("#up")) {
				face.setTintIndex(biomeTintUp);
				
				if(this.tintsUp != null && this.tintsUp.length > 0 && face.getTintIndex() < 0)
					face.setFaceColour(this.tintsUp[0].getR(), this.tintsUp[0].getG(), this.tintsUp[0].getB());
			}else if(face.getTexture().equals("#down")) {
				face.setTintIndex(biomeTintDown);
				
				if(this.tintsDown != null && this.tintsDown.length > 0 && face.getTintIndex() < 0)
					face.setFaceColour(this.tintsDown[0].getR(), this.tintsDown[0].getG(), this.tintsDown[0].getB());
			}
		}
		
		if(this.customModelScale != 1f) {
			// Scale the model. The pivot is the bottom center of the block.
			model.scale(this.customModelScale, new Vector3f(8f, 0f, 8f));
		}
		
		return model;
	}
	
	private Matrix getTransform(NbtTagCompound properties, int x, int y, int z, int permutation) {
		int rotation = 0;
		NbtTag rotTag = properties.get("rotation");
		if(rotTag != null)
			rotation = rotTag.asInt();
		
		Matrix mat = null;
		
		if(rotation != 0) {
			int yawIndex = rotation % 4;
			int pitchIndex = (rotation / 4) % 4;
			int rollIndex = (rotation / 16) % 4;
			
			Matrix rotMatrix = Matrix.rotate(pitchIndex * -90f, yawIndex * -90f, rollIndex * -90f, new Vector3f(8f, 8f, 8f));
			mat = rotMatrix;
		}
		
		if(this.randomRotation == RandomRotation.YawPitchRollStep1) {
			float randX = Noise.get(x+1, y, z) * 360f;
			float randY = Noise.get(x+2, y+2, z) * 360f;
			float randZ = Noise.get(x+3, y, z+3) * 360f;
			
			Matrix rotMatrix = Matrix.rotate(randX, randY, randZ, new Vector3f(8f, 8f, 8f));
			if(mat == null)
				mat = rotMatrix;
			else
				mat = mat.mult(rotMatrix);
		}else if(this.randomRotation == RandomRotation.YawStep1) {
			float randY = getYawRotationForPermutation(permutation);
			
			Matrix rotMatrix = Matrix.rotate(0, randY, 0, new Vector3f(8f, 8f, 8f));
			if(mat == null)
				mat = rotMatrix;
			else
				mat = mat.mult(rotMatrix);
		}else if(this.randomRotation == RandomRotation.YawStep1XZ) {
			float randY = getYawRotationForPermutation(permutation);
			
			Matrix rotMatrix = Matrix.rotate(0, randY, 0, new Vector3f(8f, 8f, 8f));
			if(mat == null)
				mat = rotMatrix;
			else
				mat = mat.mult(rotMatrix);
		}else if(this.randomRotation == RandomRotation.YawStep90) {
			float randY = getYawRotationForPermutation(permutation);
			
			Matrix rotMatrix = Matrix.rotate(0, randY, 0, new Vector3f(8f, 8f, 8f));
			if(mat == null)
				mat = rotMatrix;
			else
				mat = mat.mult(rotMatrix);
		}
		
		return mat;
	}
	
	private int getNumPermutations() {
		int permutations = 1;
		if(this.drawType.equals("Cube") || this.drawType.equals("CubeWithModel")) {
			permutations *= cubeTextures.size();
		}
		if((this.drawType.equals("Model") || this.drawType.equals("CubeWithModel")) && customModelId != null) {
			permutations *= customModelTextures.size();
		}
		if(this.randomRotation == RandomRotation.YawPitchRollStep1) {
			permutations *= 1;
		}else if(this.randomRotation == RandomRotation.YawStep1) {
			permutations *= 36;
		}else if(this.randomRotation == RandomRotation.YawStep1XZ) {
			permutations *= 36;
		}else if(this.randomRotation == RandomRotation.YawStep90) {
			permutations *= 4;
		}
		return permutations;
	}
	
	private CubeTextures getCubeTextureForPermutation(int permutation) {
		if((this.drawType.equals("Cube") || this.drawType.equals("CubeWithModel")) && cubeTextures.size() > 0) {
			return cubeTextures.get(permutation % cubeTextures.size());
		}
		return null;
	}
	
	private ModelTexture getModelTextureForPermutation(int permutation) {
		if((this.drawType.equals("Cube") || this.drawType.equals("CubeWithModel")) && cubeTextures.size() > 0) {
			permutation /= cubeTextures.size();
		}
		if((this.drawType.equals("Model") || this.drawType.equals("CubeWithModel")) && customModelId != null) {
			return customModelTextures.get(permutation % customModelTextures.size());
		}
		return null;
	}
	
	private float getYawRotationForPermutation(int permutation) {
		if((this.drawType.equals("Cube") || this.drawType.equals("CubeWithModel")) && cubeTextures.size() > 0) {
			permutation /= cubeTextures.size();
		}
		if((this.drawType.equals("Model") || this.drawType.equals("CubeWithModel")) && customModelId != null) {
			permutation /= customModelTextures.size();
		}
		if(this.randomRotation == RandomRotation.YawPitchRollStep1) {
			return 0f;
		}else if(this.randomRotation == RandomRotation.YawStep1) {
			return ((float) permutation) * 10f;
		}else if(this.randomRotation == RandomRotation.YawStep1XZ) {
			return ((float) permutation) * 10f;
		}else if(this.randomRotation == RandomRotation.YawStep90) {
			return ((float) permutation) * 90f;
		}
		return 0f;
	}
	
}
