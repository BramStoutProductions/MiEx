package nl.bramstout.mcworldexporter.resourcepack;

public class PaintingVariant {

	private String id;
	private String assetId;
	private int width;
	private int height;
	
	public PaintingVariant(String id, String assetId, int width, int height) {
		this.id = id;
		this.assetId = assetId;
		this.width = width;
		this.height = height;
	}

	public String getId() {
		return id;
	}

	public String getAssetId() {
		return assetId;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
	public String getAssetPath() {
		String[] tokens = assetId.split(":");
		return tokens[0] + ":painting/" + tokens[1];
	}
	
}
