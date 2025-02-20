package nl.bramstout.mcworldexporter.export.processors;

import nl.bramstout.mcworldexporter.export.LargeDataOutputStream;
import nl.bramstout.mcworldexporter.export.Mesh;

public class WriteProcessor implements MeshProcessors.IMeshProcessor{
	
	private LargeDataOutputStream dos;
	
	public WriteProcessor(LargeDataOutputStream dos) {
		this.dos = dos;
	}
	
	@Override
	public void process(Mesh mesh, MeshProcessors manager) throws Exception{
		try {
			mesh.write(dos);
		}catch(Exception ex) {
			throw new RuntimeException(ex);
		}
		manager.processNext(mesh, this);
	}

}
