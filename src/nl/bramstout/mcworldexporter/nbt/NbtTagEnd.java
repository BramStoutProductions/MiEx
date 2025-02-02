
package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagEnd extends NbtTag{

	public static final byte ID = 0;
	
	@Override
	protected void _free() {}

	@Override
	public NbtTag copy() {
		return NbtTag.newTag(ID, name);
	}

	@Override
	public byte getId() {
		return ID;
	}
	
	@Override
	public String asString() {
		return "";
	}

	@Override
	protected void read(DataInput dis) throws Exception {}

}
