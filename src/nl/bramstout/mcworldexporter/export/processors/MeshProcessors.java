package nl.bramstout.mcworldexporter.export.processors;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.export.Mesh;

public class MeshProcessors {
	
	public static interface IMeshProcessor{
		
		public void process(Mesh mesh, MeshProcessors manager) throws Exception;
		
	}
	
	private List<IMeshProcessor> processorChain;
	
	public MeshProcessors() {
		this.processorChain = new ArrayList<IMeshProcessor>();
	}
	
	public MeshProcessors addProcessor(IMeshProcessor processor) {
		processorChain.add(processor);
		return this;
	}
	
	public void process(Mesh mesh) throws Exception{
		if(processorChain.size() == 0)
			return;
		processorChain.get(0).process(mesh, this);
	}
	
	protected void processNext(Mesh mesh, IMeshProcessor currentProcessor) throws Exception {
		boolean found = false;
		for(int i = 0; i < processorChain.size(); ++i) {
			if(processorChain.get(i) == currentProcessor) {
				found = true;
				continue;
			}
			if(found) {
				processorChain.get(i).process(mesh, this);
				return;
			}
		}
	}
	
}
