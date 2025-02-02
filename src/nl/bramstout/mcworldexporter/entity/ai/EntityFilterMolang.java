package nl.bramstout.mcworldexporter.entity.ai;

import nl.bramstout.mcworldexporter.entity.Entity;
import nl.bramstout.mcworldexporter.molang.MolangContext;
import nl.bramstout.mcworldexporter.molang.MolangQuery;
import nl.bramstout.mcworldexporter.molang.MolangScript;

public class EntityFilterMolang extends EntityFilter{

	private MolangScript test;
	
	public EntityFilterMolang(MolangScript test) {
		this.test = test;
	}
	
	@Override
	public boolean testFilter(Entity entity) {
		MolangContext context = new MolangContext(new MolangQuery(entity.getId(), entity.getProperties(), 
																	entity.getX(), entity.getY(), entity.getZ()), entity.getRandom());
		context.setVariableDict(entity.getVariables());
		return test.eval(context).asBoolean(context);
	}

}
