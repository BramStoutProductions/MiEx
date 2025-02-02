package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.molang.MolangExpression;
import nl.bramstout.mcworldexporter.molang.MolangExpression.MolangConstantExpression;
import nl.bramstout.mcworldexporter.molang.MolangParser;
import nl.bramstout.mcworldexporter.molang.MolangValue;

public class BlockStatePart {
	
	private MolangExpression condition;
	private String geometry;
	private Map<String, MolangExpression> boneVisibility;
	private Map<String, String> materialInstances;
	private Vector3f translation;
	private Vector3f rotation;
	private Vector3f rotationPivot;
	private Vector3f scale;
	private Vector3f scalePivot;
	private boolean transparency;
	
	public BlockStatePart(String condition, JsonObject components) {
		this.condition = new MolangConstantExpression(new MolangValue(true));
		if(condition != null) {
			this.condition = MolangParser.parse(condition);
		}
		this.geometry = null;
		this.boneVisibility = null;
		this.materialInstances = null;
		this.translation = null;
		this.rotation = null;
		this.rotationPivot = null;
		this.scale = null;
		this.scalePivot = null;
		this.transparency = false;
		
		if(components.has("minecraft:geometry")) {
			JsonElement geometryEl = components.get("minecraft:geometry");
			if(geometryEl.isJsonPrimitive()) {
				this.geometry = geometryEl.getAsString();
			}else if(geometryEl.isJsonObject()) {
				JsonObject geometryObj = geometryEl.getAsJsonObject();
				if(geometryObj.has("identifier")) {
					this.geometry = geometryObj.get("identifier").getAsString();
				}
				if(geometryObj.has("bone_visibility")) {
					this.boneVisibility = new HashMap<String, MolangExpression>();
					for(Entry<String, JsonElement> entry : geometryObj.get("bone_visibility").getAsJsonObject().entrySet()) {
						if(entry.getValue().getAsJsonPrimitive().isBoolean()) {
							this.boneVisibility.put(entry.getKey(), 
									new MolangConstantExpression(new MolangValue(entry.getValue().getAsBoolean())));
						}else if(entry.getValue().getAsJsonPrimitive().isString()) {
							this.boneVisibility.put(entry.getKey(), 
									MolangParser.parse(entry.getValue().getAsString()));
						}
					}
				}
			}
		}
		if(components.has("minecraft:material_instances")) {
			this.materialInstances = new HashMap<String, String>();
			JsonObject materialInstancesObj = components.get("minecraft:material_instances").getAsJsonObject();
			for(Entry<String, JsonElement> entry : materialInstancesObj.entrySet()) {
				if(entry.getValue().isJsonPrimitive()) {
					String varName = entry.getKey();
					if(!varName.startsWith("#") && !varName.equals("*"))
						varName = "#" + varName;
					this.materialInstances.put(varName, entry.getValue().getAsString());
				}else if(entry.getValue().isJsonObject()) {
					JsonObject entryObj = entry.getValue().getAsJsonObject();
					if(entryObj.has("texture")) {
						String varName = entry.getKey();
						if(!varName.startsWith("#") && !varName.equals("*"))
							varName = "#" + varName;
						this.materialInstances.put(varName, entryObj.get("texture").getAsString());
					}
					if(entryObj.has("render_method")) {
						String renderMethod = entryObj.get("render_method").getAsString();
						if(renderMethod.equals("blend") || renderMethod.equals("alpha_test"))
							this.transparency = true;
					}
				}
			}
		}
		if(components.has("minecraft:transformation")) {
			JsonObject transformationObj = components.get("minecraft:transformation").getAsJsonObject();
			
			this.translation = new Vector3f();
			this.rotation = new Vector3f();
			this.rotationPivot = new Vector3f(8f, 8f, 8f);
			this.scale = new Vector3f(1f);
			this.scalePivot = new Vector3f(8f, 8f, 8f);
			if(transformationObj.has("translation"))
				this.translation = readVector3f(transformationObj.get("translation"));
			if(transformationObj.has("rotation"))
				this.rotation = readVector3f(transformationObj.get("rotation"));
			if(transformationObj.has("rotation_pivot")) {
				this.rotationPivot = readVector3f(transformationObj.get("rotation_pivot"));
				// MiEx origin is bottom left of the block
				// but that of Bedrock Edition is center, so offset the pivot
				this.rotationPivot = this.rotationPivot.add(new Vector3f(8f, 8f, 8f));
			}
			if(transformationObj.has("scale"))
				this.scale = readVector3f(transformationObj.get("scale"));
			if(transformationObj.has("scale_pivot")) {
				this.scalePivot = readVector3f(transformationObj.get("scale_pivot"));
				// MiEx origin is bottom left of the block
				// but that of Bedrock Edition is center, so offset the pivot
				this.scalePivot = this.scalePivot.add(new Vector3f(8f, 8f, 8f));
			}
		}
	}
	
	private Vector3f readVector3f(JsonElement el) {
		if(el == null)
			return new Vector3f();
		
		JsonArray array = el.getAsJsonArray();
		if(array.size() <= 0)
			return new Vector3f();
		if(array.size() < 3)
			return new Vector3f(array.get(0).getAsFloat());
		return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
	}

	public MolangExpression getCondition() {
		return condition;
	}

	public String getGeometry() {
		return geometry;
	}

	public Map<String, MolangExpression> getBoneVisibility() {
		return boneVisibility;
	}

	public Map<String, String> getMaterialInstances() {
		return materialInstances;
	}

	public Vector3f getTranslation() {
		return translation;
	}

	public Vector3f getRotation() {
		return rotation;
	}

	public Vector3f getRotationPivot() {
		return rotationPivot;
	}

	public Vector3f getScale() {
		return scale;
	}

	public Vector3f getScalePivot() {
		return scalePivot;
	}
	
	public boolean hasTransparency() {
		return transparency;
	}

}
