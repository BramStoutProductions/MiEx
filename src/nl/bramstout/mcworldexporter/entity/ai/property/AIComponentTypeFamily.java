package nl.bramstout.mcworldexporter.entity.ai.property;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.entity.ai.AIComponent;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;

public class AIComponentTypeFamily extends AIComponent{

	/**
	 * List of families that this entity belongs to.
	 */
	public List<String> families;
	
	public AIComponentTypeFamily(String name) {
		super(name, PriorityGroup.NONE, 0, 0);
		families = new ArrayList<String>();
	}
	
	@Override
	public boolean tick(Entity entity, float time, float deltaTime) {
		NbtTag[] familyTags = new NbtTag[families.size()];
		for(int i = 0; i < familyTags.length; ++i)
			familyTags[i] = NbtTagString.newNonPooledInstance("", families.get(i));
		entity.getProperties().addElement(NbtTagList.newNonPooledInstance("TypeFamilies", familyTags));
		return true;
	}
	
	@Override
	public void disabledTick(Entity entity, float time, float deltaTime) {
		entity.getProperties().addElement(NbtTagList.newNonPooledInstance("TypeFamilies"));
	}

}
