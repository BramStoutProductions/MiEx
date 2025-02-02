package nl.bramstout.mcworldexporter.resourcepack.bedrock;

import nl.bramstout.mcworldexporter.model.Direction;
import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.resourcepack.ModelHandler;

public class ModelHandlerFullBlock extends ModelHandler{

	@Override
	public void getGeometry(Model model) {
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
				-8f,  0f, -8f,
				 8f, 16f, -8f
		};
		model.addFace(points, uvs, Direction.NORTH, "#north");
		
		// South
		points = new float[] {
				-8f,  0f, 8f,
				 8f, 16f, 8f
		};
		model.addFace(points, uvs, Direction.SOUTH, "#south");
		
		// West
		points = new float[] {
				-8f,  0f, -8f,
				-8f, 16f,  8f
		};
		model.addFace(points, uvs, Direction.WEST, "#west");
		
		// East
		points = new float[] {
				8f,  0f, -8f,
				8f, 16f,  8f
		};
		model.addFace(points, uvs, Direction.EAST, "#east");
		
		// Up
		points = new float[] {
				-8f, 16f, -8f,
				 8f, 16f,  8f
		};
		model.addFace(points, uvs, Direction.UP, "#up");
		
		// Down
		points = new float[] {
				-8f, 0f, -8f,
				 8f, 0f,  8f
		};
		model.addFace(points, uvs, Direction.DOWN, "#down");
	}

}
