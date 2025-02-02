package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

public class NbtTagList extends NbtTag{
	
	public static final byte ID = 9;
	
	public static NbtTagList newInstance(String name) {
		return (NbtTagList) NbtTag.newTag(ID, name);
	}
	
	public static NbtTagList newInstance(String name, NbtTag[] data) {
		NbtTagList tag = (NbtTagList) NbtTag.newTag(ID, name);
		tag.type = 0;
		if(data != null && data.length > 0)
			tag.type = data[0].getId();
		tag.data = data;
		return tag;
	}
	
	public static NbtTagList newNonPooledInstance(String name) {
		return (NbtTagList) NbtTag.newNonPooledTag(ID, name);
	}
	
	public static NbtTagList newNonPooledInstance(String name, NbtTag[] data) {
		NbtTagList tag = (NbtTagList) NbtTag.newNonPooledTag(ID, name);
		tag.type = 0;
		if(data != null && data.length > 0)
			tag.type = data[0].getId();
		tag.data = data;
		return tag;
	}

	private byte type;
	private NbtTag[] data;
	
	@Override
	protected void _free() {
		if(data != null)
			for(NbtTag element : data)
				if(element != null)
					element.free();
		data = null;
		this.type = 0;
	}
	
	public NbtTag get(int i) {
		if(data != null && i >= 0 && i < data.length)
			return data[i];
		return null;
	}
	
	public byte getDataType() {
		return type;
	}
	
	public int getSize() {
		if(data != null)
			return data.length;
		return 0;
	}
	
	public NbtTag[] getData() {
		return data;
	}
	
	public void setData(NbtTag[] data) {
		_free();
		this.type = 0;
		if(data != null) {
			if(data.length > 0)
				this.type = data[0].getId();
			this.data = data;
		}
	}

	@Override
	public NbtTag copy() {
		NbtTagList copy = (NbtTagList) NbtTag.newTag(ID, name);
		int size = getSize();
		copy.type = type;
		copy.data = new NbtTag[size];
		for(int i = 0; i < size; ++i)
			if(data[i] != null)
				copy.data[i] = data[i].copy();
		return copy;
	}

	@Override
	public byte getId() {
		return ID;
	}

	@Override
	protected void read(DataInput dis) throws Exception {
		type = dis.readByte();
		int size = dis.readInt();
		data = new NbtTag[size];
		for(int i = 0; i < size; ++i) {
			data[i] = NbtTag.newTag(type, "");
			data[i].read(dis);
		}
	}
	
	@Override
	public String asString() {
		return "";
	}
	
	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + type;
		if(data != null)
			for(int i = 0; i < data.length; ++i)
				if(data[i] != null)
					result = 31 * result + data[i].hashCode();
		return result;
	}

}
