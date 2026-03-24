package nl.bramstout.mcworldexporter.locators;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.math.Vector3f;

public class PointLocators extends Locators{

	private List<Vector3f> points;
	
	public PointLocators(String name, JsonObject data) {
		super(name, data);
		points = new ArrayList<Vector3f>();
		
		if(data.has("points")) {
			for(JsonElement el : data.getAsJsonArray("points")) {
				if(el.isJsonArray() && el.getAsJsonArray().size() >= 3) {
					points.add(new Vector3f(
							el.getAsJsonArray().get(0).getAsFloat(),
							el.getAsJsonArray().get(1).getAsFloat(),
							el.getAsJsonArray().get(2).getAsFloat()
							));
				}
			}
		}
	}
	
	public List<Vector3f> getPoints(){
		return points;
	}
	
}
