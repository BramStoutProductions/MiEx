package nl.bramstout.mcworldexporter.nbt;

import java.io.DataInput;

import nl.bramstout.mcworldexporter.MemoryPool;
import nl.bramstout.mcworldexporter.Poolable;

public abstract class NbtTag extends Poolable{
	
	protected String name;
	protected int refCount;
	
	protected NbtTag() {
		name = "";
		refCount = 0;
	}
	
	public void acquireOwnership() {
		refCount++;
	}
	
	protected abstract void _free();
	
	public void free() {
		refCount--;
		if(refCount == 0) {
			_free();
			
			MemoryPool<? extends NbtTag> pool = getPool(getId());
			if(pool != null)
				pool.free(this);
		}
	}
	
	public abstract NbtTag copy();
	
	public abstract byte getId();
	
	protected abstract void read(DataInput dis) throws Exception;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public abstract String asString();
	
	
	
	private static final MemoryPool<NbtTagByteArray> POOL_BYTE_ARRAY = new MemoryPool<NbtTagByteArray>(NbtTagByteArray.class);
	private static final MemoryPool<NbtTagByte> POOL_BYTE = new MemoryPool<NbtTagByte>(NbtTagByte.class);
	private static final MemoryPool<NbtTagCompound> POOL_COMPOUND = new MemoryPool<NbtTagCompound>(NbtTagCompound.class);
	private static final MemoryPool<NbtTagDouble> POOL_DOUBLE = new MemoryPool<NbtTagDouble>(NbtTagDouble.class);
	private static final MemoryPool<NbtTagEnd> POOL_END = new MemoryPool<NbtTagEnd>(NbtTagEnd.class);
	private static final MemoryPool<NbtTagFloat> POOL_FLOAT = new MemoryPool<NbtTagFloat>(NbtTagFloat.class);
	private static final MemoryPool<NbtTagInt> POOL_INT = new MemoryPool<NbtTagInt>(NbtTagInt.class);
	private static final MemoryPool<NbtTagIntArray> POOL_INT_ARRAY = new MemoryPool<NbtTagIntArray>(NbtTagIntArray.class);
	private static final MemoryPool<NbtTagList> POOL_LIST = new MemoryPool<NbtTagList>(NbtTagList.class);
	private static final MemoryPool<NbtTagLong> POOL_LONG = new MemoryPool<NbtTagLong>(NbtTagLong.class);
	private static final MemoryPool<NbtTagLongArray> POOL_LONG_ARRAY = new MemoryPool<NbtTagLongArray>(NbtTagLongArray.class);
	private static final MemoryPool<NbtTagShort> POOL_SHORT = new MemoryPool<NbtTagShort>(NbtTagShort.class);
	private static final MemoryPool<NbtTagString> POOL_STRING = new MemoryPool<NbtTagString>(NbtTagString.class);
	
	private static MemoryPool<? extends NbtTag> getPool(byte tagId){
		switch(tagId) {
		case NbtTagByteArray.ID:
			return POOL_BYTE_ARRAY;
		case NbtTagByte.ID:
			return POOL_BYTE;
		case NbtTagCompound.ID:
			return POOL_COMPOUND;
		case NbtTagDouble.ID:
			return POOL_DOUBLE;
		case NbtTagEnd.ID:
			return POOL_END;
		case NbtTagFloat.ID:
			return POOL_FLOAT;
		case NbtTagInt.ID:
			return POOL_INT;
		case NbtTagIntArray.ID:
			return POOL_INT_ARRAY;
		case NbtTagList.ID:
			return POOL_LIST;
		case NbtTagLong.ID:
			return POOL_LONG;
		case NbtTagLongArray.ID:
			return POOL_LONG_ARRAY;
		case NbtTagShort.ID:
			return POOL_SHORT;
		case NbtTagString.ID:
			return POOL_STRING;
		default:
			return null;
		}
	}
	
	public static NbtTag newTag(byte tagId, String name) {
		MemoryPool<? extends NbtTag> pool = getPool(tagId);
		if(pool == null)
			return null;
		NbtTag tag = (NbtTag) pool.alloc();
		tag.refCount = 1;
		tag.name = name;
		return tag;
	}
	
	private static NbtTag newInstance(byte tagId) {
		switch(tagId) {
		case NbtTagByteArray.ID:
			return new NbtTagByteArray();
		case NbtTagByte.ID:
			return new NbtTagByte();
		case NbtTagCompound.ID:
			return new NbtTagCompound();
		case NbtTagDouble.ID:
			return new NbtTagDouble();
		case NbtTagEnd.ID:
			return new NbtTagEnd();
		case NbtTagFloat.ID:
			return new NbtTagFloat();
		case NbtTagInt.ID:
			return new NbtTagInt();
		case NbtTagIntArray.ID:
			return new NbtTagIntArray();
		case NbtTagList.ID:
			return new NbtTagList();
		case NbtTagLong.ID:
			return new NbtTagLong();
		case NbtTagLongArray.ID:
			return new NbtTagLongArray();
		case NbtTagShort.ID:
			return new NbtTagShort();
		case NbtTagString.ID:
			return new NbtTagString();
		default:
			return null;
		}
	}
	
	public static NbtTag newNonPooledTag(byte tagId, String name) {
		NbtTag tag = newInstance(tagId);
		tag.refCount = 0;
		tag.name = name;
		return tag;
	}
	
	public static NbtTag readFromStream(DataInput dis) throws Exception{
		byte type = dis.readByte();
		
		String name = "";
		if(type > 0)
			name = dis.readUTF();
		
		NbtTag tag = newTag(type, name);
		try {
			tag.read(dis);
		}catch(Exception ex) {
			tag.free();
			throw new RuntimeException(ex);
		}
		return tag;
	}
	
}
