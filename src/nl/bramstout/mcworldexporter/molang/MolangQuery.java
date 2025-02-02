package nl.bramstout.mcworldexporter.molang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.math.Matrix;
import nl.bramstout.mcworldexporter.math.Vector3f;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangArray;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangDictionary;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangFunction;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangObject;
import nl.bramstout.mcworldexporter.nbt.NbtTag;
import nl.bramstout.mcworldexporter.nbt.NbtTagByte;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;
import nl.bramstout.mcworldexporter.nbt.NbtTagFloat;
import nl.bramstout.mcworldexporter.nbt.NbtTagInt;
import nl.bramstout.mcworldexporter.nbt.NbtTagList;
import nl.bramstout.mcworldexporter.nbt.NbtTagLong;
import nl.bramstout.mcworldexporter.nbt.NbtTagShort;
import nl.bramstout.mcworldexporter.nbt.NbtTagString;
import nl.bramstout.mcworldexporter.resourcepack.Biome;
import nl.bramstout.mcworldexporter.resourcepack.Tags;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.Block;
import nl.bramstout.mcworldexporter.world.BlockRegistry;

public class MolangQuery extends MolangObject{

	public String resourceId;
	public NbtTagCompound properties;
	public float x;
	public float y;
	public float z;
	
	private class AboveTopSolid extends MolangScript{
		public AboveTopSolid() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int height = MCWorldExporter.getApp().getWorld().getHeight((int) x, (int) y);
			return new MolangValue(height + 1);
		}
	}
	private class All extends MolangScript{
		public All() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue firstVal = context.getTempDict().getField("arg_0");
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0")) {
					if(!entry.getValue().equal(context, firstVal))
						return new MolangValue(0f);
				}
			}
			return new MolangValue(1f);
		}
	}
	private class AllTags extends MolangScript{
		public AllTags() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			List<String> tags = Tags.getTagsForResourceId(resourceId);
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_")) {
					if(!tags.contains(entry.getValue().asString(context)))
						return new MolangValue(0f);
				}
			}
			return new MolangValue(1f);
		}
	}
	private class Any extends MolangScript{
		public Any() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue firstVal = context.getTempDict().getField("arg_0");
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0")) {
					if(entry.getValue().equal(context, firstVal))
						return new MolangValue(1f);
				}
			}
			return new MolangValue(0f);
		}
	}
	private class AnyTags extends MolangScript{
		public AnyTags() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			List<String> tags = Tags.getTagsForResourceId(resourceId);
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_")) {
					if(tags.contains(entry.getValue().asString(context)))
						return new MolangValue(1f);
				}
			}
			return new MolangValue(0f);
		}
	}
	private class HasAnyFamily extends MolangScript{
		public HasAnyFamily() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			List<String> families = new ArrayList<String>();
			NbtTagList familyTag = (NbtTagList) properties.get("TypeFamilies");
			if(familyTag != null) {
				for(NbtTag tag : familyTag.getData()) {
					families.add(((NbtTagString) tag).getData());
				}
			}
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_")) {
					if(families.contains(entry.getValue().asString(context)))
						return new MolangValue(1f);
				}
			}
			return new MolangValue(0f);
		}
	}
	private class ApproxEq extends MolangScript{
		public ApproxEq() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue firstVal = context.getTempDict().getField("arg_0");
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0")) {
					try {
						if(Math.abs(entry.getValue().asNumber(context) - firstVal.asNumber(context)) >= 0.0000001f)
							return new MolangValue(0f);
					}catch(Exception ex) {
						if(!entry.getValue().equal(context, firstVal))
							return new MolangValue(0f);
					}
				}
			}
			return new MolangValue(0f);
		}
	}
	private class BlockHasAllTags extends MolangScript{
		public BlockHasAllTags() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int x = (int) context.getTempDict().getField("arg_0").asNumber(context);
			int y = (int) context.getTempDict().getField("arg_1").asNumber(context);
			int z = (int) context.getTempDict().getField("arg_2").asNumber(context);
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
			Block block = BlockRegistry.getBlock(blockId);
			List<String> tags = Tags.getTagsForResourceId(block.getName());
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0") && 
						!entry.getKey().equals("arg_1") && !entry.getKey().equals("arg_2")) {
					String tag = entry.getValue().asString(context);
					if(!tag.contains(":"))
						tag = "minecraft:" + tag;
					if(!tags.contains(tag)) {
						if(!tags.contains(tag.replace(":", ":block/"))) {
							if(!tags.contains(tag.replace(":", ":blocks/"))) {
								return new MolangValue(0f);
							}
						}
					}
				}
			}
			return new MolangValue(1f);
		}
	}
	private class BlockHasAnyTags extends MolangScript{
		public BlockHasAnyTags() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int x = (int) context.getTempDict().getField("arg_0").asNumber(context);
			int y = (int) context.getTempDict().getField("arg_1").asNumber(context);
			int z = (int) context.getTempDict().getField("arg_2").asNumber(context);
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
			Block block = BlockRegistry.getBlock(blockId);
			List<String> tags = Tags.getTagsForResourceId(block.getName());
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0") && 
						!entry.getKey().equals("arg_1") && !entry.getKey().equals("arg_2")) {
					String tag = entry.getValue().asString(context);
					if(!tag.contains(":"))
						tag = "minecraft:" + tag;
					if(tags.contains(tag))
						return new MolangValue(1f);
					if(tags.contains(tag.replace(":", ":block/")))
						return new MolangValue(1f);
					if(tags.contains(tag.replace(":", ":blocks/")))
						return new MolangValue(1f);
				}
			}
			return new MolangValue(0f);
		}
	}
	private class BlockNeighbourHasAllTags extends MolangScript{
		public BlockNeighbourHasAllTags() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int bx = (int) context.getTempDict().getField("arg_0").asNumber(context);
			int by = (int) context.getTempDict().getField("arg_1").asNumber(context);
			int bz = (int) context.getTempDict().getField("arg_2").asNumber(context);
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(((int) x) + bx, ((int) y) + by, ((int) z) + bz);
			Block block = BlockRegistry.getBlock(blockId);
			List<String> tags = Tags.getTagsForResourceId(block.getName());
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0") && 
						!entry.getKey().equals("arg_1") && !entry.getKey().equals("arg_2")) {
					String tag = entry.getValue().asString(context);
					if(!tag.contains(":"))
						tag = "minecraft:" + tag;
					if(!tags.contains(tag)) {
						if(!tags.contains(tag.replace(":", ":block/"))) {
							if(!tags.contains(tag.replace(":", ":blocks/"))) {
								return new MolangValue(0f);
							}
						}
					}
				}
			}
			return new MolangValue(1f);
		}
	}
	private class BlockNeighbourHasAnyTags extends MolangScript{
		public BlockNeighbourHasAnyTags() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int bx = (int) context.getTempDict().getField("arg_0").asNumber(context);
			int by = (int) context.getTempDict().getField("arg_1").asNumber(context);
			int bz = (int) context.getTempDict().getField("arg_2").asNumber(context);
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(((int) x) + bx, ((int) y) + by, ((int) z) + bz);
			Block block = BlockRegistry.getBlock(blockId);
			List<String> tags = Tags.getTagsForResourceId(block.getName());
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0") && 
						!entry.getKey().equals("arg_1") && !entry.getKey().equals("arg_2")) {
					String tag = entry.getValue().asString(context);
					if(!tag.contains(":"))
						tag = "minecraft:" + tag;
					if(tags.contains(tag))
						return new MolangValue(1f);
					if(tags.contains(tag.replace(":", ":block/")))
						return new MolangValue(1f);
					if(tags.contains(tag.replace(":", ":blocks/")))
						return new MolangValue(1f);
				}
			}
			return new MolangValue(0f);
		}
	}
	private class BlockProperty extends MolangScript{
		public BlockProperty() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String propertyName = context.getTempDict().getField("arg_0").asString(context);
			NbtTag tag = properties.get(propertyName);
			if(tag == null)
				return new MolangValue();
			
			return MolangValue.fromNBT(tag);
		}
	}
	private class Count extends MolangScript{
		public Count() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int count = 0;
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_")) {
					if(entry.getValue().getImpl() instanceof MolangArray) {
						count += ((MolangArray)entry.getValue().getImpl()).get().size();
					}else {
						count += 1;
					}
				}
			}
			return new MolangValue(count);
		}
	}
	private class GetName extends MolangScript{
		public GetName() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(resourceId);
		}
	}
	private class HasBiomeTag extends MolangScript{
		public HasBiomeTag() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int biomeId = MCWorldExporter.getApp().getWorld().getBiomeId((int) x, (int) y, (int) z);
			Biome biome = BiomeRegistry.getBiome(biomeId);
			List<String> tags = Tags.getTagsForResourceId(biome.getName());
			String queryTag = context.getTempDict().getField("arg_0").asString(context);
			if(!queryTag.contains(":"))
				queryTag = "minecraft:" + queryTag;
			if(tags.contains(queryTag))
				return new MolangValue(true);
			if(tags.contains(queryTag.replace(":", ":worldgen/biome")))
				return new MolangValue(true);
			if(queryTag.equals(biome.getName()))
				return new MolangValue(true);
			return new MolangValue(false);
		}
	}
	private class HasBiome extends MolangScript{
		public HasBiome() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int biomeId = MCWorldExporter.getApp().getWorld().getBiomeId((int) x, (int) y, (int) z);
			Biome biome = BiomeRegistry.getBiome(biomeId);
			String queryTag = context.getTempDict().getField("arg_0").asString(context);
			return new MolangValue(biome.getName().equalsIgnoreCase(queryTag));
		}
	}
	private class HasBlockProperty extends MolangScript{
		public HasBlockProperty() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String queryProperty = context.getTempDict().getField("arg_0").asString(context);
			return new MolangValue(properties.get(queryProperty) != null);
		}
	}
	private class GetBlockName extends MolangScript{
		public GetBlockName() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
			Block block = BlockRegistry.getBlock(blockId);
			return new MolangValue(block.getName());
		}
	}
	private class Heightmap extends MolangScript{
		public Heightmap() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int height = MCWorldExporter.getApp().getWorld().getHeight((int) x, (int) y);
			return new MolangValue(height);
		}
	}
	private class InRange extends MolangScript{
		public InRange() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("arg_0").asNumber(context);
			float min = context.getTempDict().getField("arg_1").asNumber(context);
			float max = context.getTempDict().getField("arg_2").asNumber(context);
			float res = value >= min && value <= max ? 1f : 0f;
			return new MolangValue(res);
		}
	}
	private class Log extends MolangScript{
		public Log() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_")) {
					System.out.print(entry.getValue().asString(context) + " ");
				}
			}
			System.out.println();
			return new MolangValue();
		}
	}
	private class Position extends MolangScript{
		public Position() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int axis = (int) context.getTempDict().getField("arg_0").asNumber(context);
			return new MolangValue(axis == 0 ? x : (axis == 1 ? y : (axis == 2) ? z : 0));
		}
	}
	
	private class GetEquippedItemName extends MolangScript{
		public GetEquippedItemName() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String hand = context.getTempDict().getField("arg_0").asString(context);
			if(hand.equalsIgnoreCase("main_hand") || hand.equals("0")) {
				NbtTagList mainHandTag = (NbtTagList) properties.get("Mainhand");
				if(mainHandTag != null) {
					NbtTagCompound mainHandTag2 = (NbtTagCompound) mainHandTag.get(0);
					NbtTagString nameTag = (NbtTagString) mainHandTag2.get("Name");
					if(nameTag != null)
						return new MolangValue(nameTag.getData());
				}
				return new MolangValue();
			}
			if(hand.equalsIgnoreCase("off_hand") || hand.equals("1")) {
				NbtTagList offHandTag = (NbtTagList) properties.get("Offhand");
				if(offHandTag != null) {
					NbtTagCompound offHandTag2 = (NbtTagCompound) offHandTag.get(0);
					NbtTagString nameTag = (NbtTagString) offHandTag2.get("Name");
					if(nameTag != null)
						return new MolangValue(nameTag.getData());
				}
				return new MolangValue();
			}
			return new MolangValue();
		}
	}
	private class IsNameAny extends MolangScript{
		public IsNameAny() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String name = "";
			NbtTagString nameTag = (NbtTagString) properties.get("CustomName");
			if(nameTag != null)
				name = nameTag.getData();
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_")) {
					if(name.equals(entry.getValue().asString(context)))
						return new MolangValue(true);
				}
			}
			return new MolangValue(false);
		}
	}
	private class IsItemNameAny extends MolangScript{
		public IsItemNameAny() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String slot = context.getTempDict().getField("arg_0").asString(context);
			int slotIndex = (int) context.getTempDict().getField("arg_1").asNumber(context);
			
			List<String> names = new ArrayList<String>();
			
			if(slot.equalsIgnoreCase("slot.weapon_mainhand") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList mainHandTag = (NbtTagList) properties.get("Mainhand");
				if(mainHandTag != null) {
					NbtTagCompound mainHandTag2 = (NbtTagCompound) mainHandTag.get(0);
					NbtTagString nameTag = (NbtTagString) mainHandTag2.get("Name");
					if(nameTag != null && !nameTag.getData().isEmpty())
						names.add(nameTag.getData());
				}
				if(names.isEmpty()) {
					NbtTagCompound itemInHandTag = (NbtTagCompound) properties.get("ItemInHand");
					if(itemInHandTag != null) {
						NbtTagString nameTag = (NbtTagString) itemInHandTag.get("Name");
						if(nameTag != null && !nameTag.getData().isEmpty())
							names.add(nameTag.getData());
					}
				}
			}else if(slot.equalsIgnoreCase("slot.weapon_offhand") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList offHandTag = (NbtTagList) properties.get("Offhand");
				if(offHandTag != null) {
					NbtTagCompound offHandTag2 = (NbtTagCompound) offHandTag.get(0);
					NbtTagString nameTag = (NbtTagString) offHandTag2.get("Name");
					if(nameTag != null && !nameTag.getData().isEmpty())
						names.add(nameTag.getData());
				}
			}else if(slot.equalsIgnoreCase("slot.armor.head") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList armorTag = (NbtTagList) properties.get("Armor");
				if(armorTag != null) {
					NbtTagCompound armorTag2 = (NbtTagCompound) armorTag.get(0);
					NbtTagString nameTag = (NbtTagString) armorTag2.get("Name");
					if(nameTag != null && !nameTag.getData().isEmpty())
						names.add(nameTag.getData());
				}
			}else if(slot.equalsIgnoreCase("slot.armor.chest") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList armorTag = (NbtTagList) properties.get("Armor");
				if(armorTag != null) {
					NbtTagCompound armorTag2 = (NbtTagCompound) armorTag.get(1);
					NbtTagString nameTag = (NbtTagString) armorTag2.get("Name");
					if(nameTag != null && !nameTag.getData().isEmpty())
						names.add(nameTag.getData());
				}
			}else if(slot.equalsIgnoreCase("slot.armor.legs") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList armorTag = (NbtTagList) properties.get("Armor");
				if(armorTag != null) {
					NbtTagCompound armorTag2 = (NbtTagCompound) armorTag.get(2);
					NbtTagString nameTag = (NbtTagString) armorTag2.get("Name");
					if(nameTag != null && !nameTag.getData().isEmpty())
						names.add(nameTag.getData());
				}
			}else if(slot.equalsIgnoreCase("slot.armor.feet") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList armorTag = (NbtTagList) properties.get("Armor");
				if(armorTag != null) {
					NbtTagCompound armorTag2 = (NbtTagCompound) armorTag.get(3);
					NbtTagString nameTag = (NbtTagString) armorTag2.get("Name");
					if(nameTag != null && !nameTag.getData().isEmpty())
						names.add(nameTag.getData());
				}
			}else if(slot.equalsIgnoreCase("slot.armor") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList armorTag = (NbtTagList) properties.get("Armor");
				if(armorTag != null) {
					for(int i = 0; i < 4; ++i) {
						NbtTagCompound armorTag2 = (NbtTagCompound) armorTag.get(i);
						NbtTagString nameTag = (NbtTagString) armorTag2.get("Name");
						if(nameTag != null && !nameTag.getData().isEmpty())
							names.add(nameTag.getData());
					}
				}
			}else if(slot.equalsIgnoreCase("slot.hotbar") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList inventoryTag = (NbtTagList) properties.get("Inventory");
				if(inventoryTag != null) {
					for(NbtTag tag : inventoryTag.getData()) {
						NbtTagCompound inventoryTag2 = (NbtTagCompound) tag;
						NbtTagByte slotTag = (NbtTagByte) inventoryTag2.get("Slot");
						if(slotTag == null)
							continue;
						if((((int) slotTag.getData()) - 27) != slotIndex && slotIndex >= 0)
							continue;
						NbtTagString nameTag = (NbtTagString) inventoryTag2.get("Name");
						if(nameTag != null && !nameTag.getData().isEmpty())
							names.add(nameTag.getData());
					}
				}
			}else if(slot.equalsIgnoreCase("slot.inventory") || slot.equalsIgnoreCase("slot.any")) {
				NbtTagList inventoryTag = (NbtTagList) properties.get("Inventory");
				if(inventoryTag != null) {
					for(NbtTag tag : inventoryTag.getData()) {
						NbtTagCompound inventoryTag2 = (NbtTagCompound) tag;
						NbtTagByte slotTag = (NbtTagByte) inventoryTag2.get("Slot");
						if(slotTag == null)
							continue;
						if(((int) slotTag.getData()) != slotIndex && slotIndex >= 0)
							continue;
						NbtTagString nameTag = (NbtTagString) inventoryTag2.get("Name");
						if(nameTag != null && !nameTag.getData().isEmpty())
							names.add(nameTag.getData());
					}
				}
			}else if(slot.equalsIgnoreCase("slot.enderchest")) {
				NbtTagList inventoryTag = (NbtTagList) properties.get("EnderChestInventory");
				if(inventoryTag != null) {
					for(NbtTag tag : inventoryTag.getData()) {
						NbtTagCompound inventoryTag2 = (NbtTagCompound) tag;
						NbtTagByte slotTag = (NbtTagByte) inventoryTag2.get("Slot");
						if(slotTag == null)
							continue;
						if(((int) slotTag.getData()) != slotIndex && slotIndex >= 0)
							continue;
						NbtTagString nameTag = (NbtTagString) inventoryTag2.get("Name");
						if(nameTag != null && !nameTag.getData().isEmpty())
							names.add(nameTag.getData());
					}
				}
			}
			
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_") && !entry.getKey().equals("arg_0") && 
														!entry.getKey().equals("arg_1")) {
					if(names.contains(entry.getValue().asString(context)))
						return new MolangValue(true);
				}
			}
			return new MolangValue(false);
		}
	}
	private class IsEating extends MolangScript{
		public IsEating() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte isEatingTag = (NbtTagByte) properties.get("IsEating");
			if(isEatingTag != null)
				return new MolangValue(isEatingTag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsRoaring extends MolangScript{
		public IsRoaring() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte isRoaringTag = (NbtTagByte) properties.get("IsRoaring");
			if(isRoaringTag != null)
				return new MolangValue(isRoaringTag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class HeadPitch extends MolangScript{
		public HeadPitch() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info != null) {
				if(info.animation != null) {
					return new MolangValue(info.animation.getAnimHeadPitch().getKeyframeAtTime(info.globalTime).value);
				}
			}
			NbtTagFloat pitchTag = (NbtTagFloat) properties.get("HeadPitch");
			if(pitchTag != null)
				return new MolangValue(pitchTag.getData());
			return new MolangValue(0f);
		}
	}
	private class HeadYaw extends MolangScript{
		public HeadYaw() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info != null) {
				if(info.animation != null) {
					return new MolangValue(info.animation.getAnimHeadYaw().getKeyframeAtTime(info.globalTime).value);
				}
			}
			NbtTagFloat yawTag = (NbtTagFloat) properties.get("HeadYaw");
			if(yawTag != null)
				return new MolangValue(yawTag.getData());
			return new MolangValue(0f);
		}
	}
	private class Pitch extends MolangScript{
		public Pitch() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info != null) {
				if(info.animation != null) {
					return new MolangValue(info.animation.getAnimPitch().getKeyframeAtTime(info.globalTime).value);
				}
			}
			NbtTagFloat pitchTag = (NbtTagFloat) properties.get("Pitch");
			if(pitchTag != null)
				return new MolangValue(pitchTag.getData());
			return new MolangValue(0f);
		}
	}
	private class Yaw extends MolangScript{
		public Yaw() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info != null) {
				if(info.animation != null) {
					return new MolangValue(info.animation.getAnimYaw().getKeyframeAtTime(info.globalTime).value);
				}
			}
			NbtTagFloat yawTag = (NbtTagFloat) properties.get("Yaw");
			if(yawTag != null)
				return new MolangValue(yawTag.getData());
			return new MolangValue(0f);
		}
	}
	private class TimeOfDay extends MolangScript{
		public TimeOfDay() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(0.5f);
		}
	}
	private class MovementDirection extends MolangScript{
		public MovementDirection() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dx = 0f;
			float dy = 0f;
			float dz = 0f;
			NbtTagList motion = (NbtTagList) properties.get("Motion");
			if(motion != null) {
				dx = ((NbtTagFloat) motion.get(0)).getData();
				dy = ((NbtTagFloat) motion.get(1)).getData();
				dz = ((NbtTagFloat) motion.get(2)).getData();
			}
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if(length > 0.0000001f) {
				dx /= length;
				dy /= length;
				dz /= length;
			}
			int axis = (int) context.getTempDict().getField("arg_0").asNumber(context);
			if(axis <= 0)
				return new MolangValue(dx);
			if(axis == 1)
				return new MolangValue(dy);
			return new MolangValue(dz);
		}
	}
	private class AngerLevel extends MolangScript{
		public AngerLevel() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte isAngryTag = (NbtTagByte) properties.get("IsAngry");
			if(isAngryTag != null)
				return new MolangValue((int) isAngryTag.getData());
			return new MolangValue(0);
		}
	}
	private class AverageFrameTime extends MolangScript{
		public AverageFrameTime() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info != null)
				return new MolangValue(info.deltaTime);
			return new MolangValue(1f / 20f);
		}
	}
	private class CapeFlapAmount extends MolangScript{
		public CapeFlapAmount() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(0f);
		}
	}
	private class CardinalFacing extends MolangScript{
		public CardinalFacing() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float pitch = 0f;
			float yaw = 0f;
			NbtTagFloat pitchTag = (NbtTagFloat) properties.get("Pitch");
			if(pitchTag != null)
				pitch = pitchTag.getData();
			
			NbtTagFloat yawTag = (NbtTagFloat) properties.get("Yaw");
			if(yawTag != null)
				yaw = yawTag.getData();
			
			if(pitch <= 45f)
				return new MolangValue(0);
			if(pitch >= 45f)
				return new MolangValue(1);
			yaw += 45f;
			yaw /= 360f;
			yaw -= Math.floor(yaw);
			yaw *= 360f;
			if(yaw < 90f)
				return new MolangValue(2);
			if(yaw < 180f)
				return new MolangValue(5);
			if(yaw < 270f)
				return new MolangValue(3);
			return new MolangValue(4);
		}
	}
	private class CardinalFacing2D extends MolangScript{
		public CardinalFacing2D() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float yaw = 0f;
			
			NbtTagFloat yawTag = (NbtTagFloat) properties.get("Yaw");
			if(yawTag != null)
				yaw = yawTag.getData();
			
			yaw += 45f;
			yaw /= 360f;
			yaw -= Math.floor(yaw);
			yaw *= 360f;
			if(yaw < 90f)
				return new MolangValue(2);
			if(yaw < 180f)
				return new MolangValue(5);
			if(yaw < 270f)
				return new MolangValue(3);
			return new MolangValue(4);
		}
	}
	private class Day extends MolangScript{
		public Day() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(1f);
		}
	}
	private class DeathTicks extends MolangScript{
		public DeathTicks() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(0f);
		}
	}
	private class DistanceFromCamera extends MolangScript{
		public DistanceFromCamera() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dx = x - MCWorldExporter.getApp().getExportBounds().getOffsetX();
			float dy = y - MCWorldExporter.getApp().getExportBounds().getOffsetY();
			float dz = z - MCWorldExporter.getApp().getExportBounds().getOffsetZ();
			return new MolangValue((float) Math.sqrt(dx * dx + dy * dy + dz * dz));
		}
	}
	private class FrameAlpha extends MolangScript{
		public FrameAlpha() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(0f);
		}
	}
	private class HadComponentGroup extends MolangScript{
		public HadComponentGroup() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String componentGroupName = context.getTempDict().getField("arg_0").asString(context);
			NbtTagList definitionsList = (NbtTagList) properties.get("definitions");
			if(definitionsList != null) {
				for(NbtTag tag : definitionsList.getData()) {
					if(((NbtTagString) tag).getData().replace("+", "").replace("-", "").equals(componentGroupName))
						return new MolangValue(true);
				}
			}
			
			return new MolangValue(false);
		}
	}
	private class HasComponent extends MolangScript{
		public HasComponent() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String componentName = context.getTempDict().getField("arg_0").asString(context);
			NbtTagList definitionsList = (NbtTagList) properties.get("ActiveComponents");
			if(definitionsList != null) {
				for(NbtTag tag : definitionsList.getData()) {
					if(((NbtTagString) tag).getData().equals(componentName))
						return new MolangValue(true);
				}
			}
			
			return new MolangValue(false);
		}
	}
	private class HasArmorSlot extends MolangScript{
		public HasArmorSlot() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int slot = (int) context.getTempDict().getField("arg_0").asNumber(context);
			NbtTagList armorTag = (NbtTagList) properties.get("Armor");
			if(armorTag != null) {
				NbtTagCompound armorTag2 = (NbtTagCompound) armorTag.get(slot);
				if(armorTag2 != null) {
					NbtTagString nameTag = (NbtTagString) armorTag2.get("Name");
					if(nameTag != null)
						if(!nameTag.getData().isEmpty())
							return new MolangValue(true);
				}
			}
			return new MolangValue(false);
		}
	}
	private class Health extends MolangScript{
		public Health() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(20f);
		}
	}
	private class IsAlive extends MolangScript{
		public IsAlive() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("Dead");
			if(tag != null)
				return new MolangValue(tag.getData() <= 0);
			return new MolangValue(true);
		}
	}
	private class IsAngry extends MolangScript{
		public IsAngry() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsAngry");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsBaby extends MolangScript{
		public IsBaby() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsBaby");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsChested extends MolangScript{
		public IsChested() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("Chested");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsGliding extends MolangScript{
		public IsGliding() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsGliding");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsIllagerCaptain extends MolangScript{
		public IsIllagerCaptain() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsIllagerCaptain");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsItemEquipped extends MolangScript{
		public IsItemEquipped() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String hand = "main_hand";
			MolangValue handArg = context.getTempDict().getField("arg_0");
			if(handArg != null)
				hand = handArg.asString(context);
			if(hand.equalsIgnoreCase("main_hand") || hand.equals("0")) {
				NbtTagList mainHandTag = (NbtTagList) properties.get("Mainhand");
				if(mainHandTag != null) {
					NbtTagCompound mainHandTag2 = (NbtTagCompound) mainHandTag.get(0);
					NbtTagString nameTag = (NbtTagString) mainHandTag2.get("Name");
					if(nameTag != null)
						if(!nameTag.getData().isEmpty())
							return new MolangValue(true);
				}
				return new MolangValue(false);
			}
			if(hand.equalsIgnoreCase("off_hand") || hand.equals("1")) {
				NbtTagList offHandTag = (NbtTagList) properties.get("Offhand");
				if(offHandTag != null) {
					NbtTagCompound offHandTag2 = (NbtTagCompound) offHandTag.get(0);
					NbtTagString nameTag = (NbtTagString) offHandTag2.get("Name");
					if(nameTag != null)
						if(!nameTag.getData().isEmpty())
							return new MolangValue(true);
				}
				return new MolangValue(false);
			}
			return new MolangValue(false);
		}
	}
	private class IsMoving extends MolangScript{
		public IsMoving() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dx = 0f;
			float dy = 0f;
			float dz = 0f;
			NbtTagList motion = (NbtTagList) properties.get("Motion");
			if(motion != null) {
				dx = ((NbtTagFloat) motion.get(0)).getData();
				dy = ((NbtTagFloat) motion.get(1)).getData();
				dz = ((NbtTagFloat) motion.get(2)).getData();
			}
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			return new MolangValue(length > 0.0001f);
		}
	}
	private class IsOnFire extends MolangScript{
		public IsOnFire() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagShort tag = (NbtTagShort) properties.get("Fire");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsOnGround extends MolangScript{
		public IsOnGround() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("OnGround");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsOnScreen extends MolangScript{
		public IsOnScreen() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(true);
		}
	}
	private class IsOrphaned extends MolangScript{
		public IsOrphaned() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsOrphaned");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsSaddled extends MolangScript{
		public IsSaddled() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("Saddled");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsScared extends MolangScript{
		public IsScared() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsScared");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsSheared extends MolangScript{
		public IsSheared() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("Sheared");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsSitting extends MolangScript{
		public IsSitting() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("Sitting");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsStunned extends MolangScript{
		public IsStunned() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsStunned");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsSwimming extends MolangScript{
		public IsSwimming() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsSwimming");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class IsTamed extends MolangScript{
		public IsTamed() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsTamed");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class LodIndex extends MolangScript{
		public LodIndex() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dx = x - MCWorldExporter.getApp().getExportBounds().getOffsetX();
			float dy = y - MCWorldExporter.getApp().getExportBounds().getOffsetY();
			float dz = z - MCWorldExporter.getApp().getExportBounds().getOffsetZ();
			float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			int counter = 0;
			for(Entry<String, MolangValue> entry : ((MolangDictionary)context.getTempDict().getImpl()).getFields().entrySet()) {
				if(entry.getKey().startsWith("arg_")) {
					if(entry.getValue().asNumber(context) <= d) {
						return new MolangValue(Integer.parseInt(entry.getKey().substring("arg_".length())));
					}
					counter++;
				}
			}
			return new MolangValue(counter);
		}
	}
	private class MarkVariant extends MolangScript{
		public MarkVariant() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagInt tag = (NbtTagInt) properties.get("MarkVariant");
			if(tag != null)
				return new MolangValue(tag.getData());
			return new MolangValue(0);
		}
	}
	private class MaxHealth extends MolangScript{
		public MaxHealth() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(20f);
		}
	}
	private class ModelScale extends MolangScript{
		public ModelScale() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagFloat tag = (NbtTagFloat) properties.get("Scale");
			if(tag != null)
				return new MolangValue(tag.getData());
			return new MolangValue(1f);
		}
	}
	private class ModifiedMoveSpeed extends MolangScript{
		public ModifiedMoveSpeed() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			/**
			 * Returns the velocity at which the entity is moving on the xz axis,
			 * but then normalised to the movement speed attribute.
			 */
			float movementSpeed = 0.1f;
			float scale = 1f;
			NbtTagFloat tag = (NbtTagFloat) properties.get("Scale");
			if(tag != null)
				scale = tag.getData();
			NbtTagFloat movementSpeedTag = (NbtTagFloat) properties.get("MovementSpeed");
			if(movementSpeedTag != null)
				movementSpeed = movementSpeedTag.getData();
			movementSpeed *= scale;
			
			float dx = 0f;
			float dz = 0f;
			NbtTagList motion = (NbtTagList) properties.get("Motion");
			if(motion != null) {
				dx = ((NbtTagFloat) motion.get(0)).getData();
				dz = ((NbtTagFloat) motion.get(2)).getData();
			}
			float length = (float) Math.sqrt(dx * dx + dz * dz);
			return new MolangValue(length / movementSpeed);
		}
	}
	private class ModifiedDistanceMoved extends MolangScript{
		public ModifiedDistanceMoved() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagFloat tag = (NbtTagFloat) properties.get("NormalisedDistanceMoved");
			if(tag != null)
				return new MolangValue(tag.getData());
			return new MolangValue(1f);
		}
	}
	private class DistanceMoved extends MolangScript{
		public DistanceMoved() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagFloat tag = (NbtTagFloat) properties.get("DistanceMoved");
			if(tag != null)
				return new MolangValue(tag.getData());
			return new MolangValue(1f);
		}
	}
	private class MoonBrightness extends MolangScript{
		public MoonBrightness() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(1f);
		}
	}
	private class MoonPhase extends MolangScript{
		public MoonPhase() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(0f);
		}
	}
	private class OnFireTime extends MolangScript{
		public OnFireTime() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagShort tag = (NbtTagShort) properties.get("Fire");
			if(tag != null)
				return new MolangValue(tag.getData() > 0 ? 1f : 0f);
			return new MolangValue(0f);
		}
	}
	private class OutOfControl extends MolangScript{
		public OutOfControl() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("IsOutOfControl");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class PositionDelta extends MolangScript{
		public PositionDelta() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dx = 0f;
			float dy = 0f;
			float dz = 0f;
			NbtTagList motion = (NbtTagList) properties.get("Motion");
			if(motion != null) {
				dx = ((NbtTagFloat) motion.get(0)).getData() / 20f;
				dy = ((NbtTagFloat) motion.get(1)).getData() / 20f;
				dz = ((NbtTagFloat) motion.get(2)).getData() / 20f;
			}
			
			int axis = (int) context.getTempDict().getField("arg_0").asNumber(context);
			if(axis <= 0)
				return new MolangValue(dx);
			if(axis == 1)
				return new MolangValue(dy);
			return new MolangValue(dz);
		}
	}
	private class ShowBottom extends MolangScript{
		public ShowBottom() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("ShowBottom");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class SkinId extends MolangScript{
		public SkinId() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagInt tag = (NbtTagInt) properties.get("SkinID");
			if(tag != null)
				return new MolangValue(tag.getData());
			return new MolangValue(0);
		}
	}
	private class Variant extends MolangScript{
		public Variant() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagInt tag = (NbtTagInt) properties.get("Variant");
			if(tag != null)
				return new MolangValue(tag.getData());
			return new MolangValue(0);
		}
	}
	private class VerticalSpeed extends MolangScript{
		public VerticalSpeed() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dy = 0f;
			NbtTagList motion = (NbtTagList) properties.get("Motion");
			if(motion != null) {
				dy = ((NbtTagFloat) motion.get(1)).getData();
			}
			
			return new MolangValue(dy);
		}
	}
	private class YawSpeed extends MolangScript{
		public YawSpeed() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(0f);
		}
	}
	private class CanClimb extends MolangScript{
		public CanClimb() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("CanClimb");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class CanFly extends MolangScript{
		public CanFly() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("CanFly");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class GroundSpeed extends MolangScript{
		public GroundSpeed() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagFloat tag = (NbtTagFloat) properties.get("MovementSpeed");
			if(tag != null)
				return new MolangValue(tag.getData() * 20f);
			return new MolangValue(1f);
		}
	}
	private class HasCollision extends MolangScript{
		public HasCollision() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("HasCollision");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class HasGravity extends MolangScript{
		public HasGravity() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagByte tag = (NbtTagByte) properties.get("HasGravity");
			if(tag != null)
				return new MolangValue(tag.getData() > 0);
			return new MolangValue(false);
		}
	}
	private class HasTarget extends MolangScript{
		public HasTarget() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("TargetID");
			if(tag != null)
				return new MolangValue(tag.getData() != 0);
			return new MolangValue(false);
		}
	}
	private class IsCharging extends MolangScript{
		public IsCharging() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("IsCharged");
			if(tag != null)
				return new MolangValue(tag.getData() != 0);
			return new MolangValue(false);
		}
	}
	private class IsCroaking extends MolangScript{
		public IsCroaking() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("IsCroaking");
			if(tag != null)
				return new MolangValue(tag.getData() != 0);
			return new MolangValue(false);
		}
	}
	private class IsIgnited extends MolangScript{
		public IsIgnited() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("IsFuseLit");
			if(tag != null)
				return new MolangValue(tag.getData() != 0);
			return new MolangValue(false);
		}
	}
	private class IsInContactWithWater extends MolangScript{
		public IsInContactWithWater() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float scale = 1f;
			NbtTagFloat tag = (NbtTagFloat) properties.get("Scale");
			if(tag != null)
				scale = tag.getData();
			float collisionBoxWidth = 0f;
			float collisionBoxHeight = 0f;
			tag = (NbtTagFloat) properties.get("CollisionBoxWidth");
			if(tag != null)
				collisionBoxWidth = tag.getData();
			tag = (NbtTagFloat) properties.get("CollisionBoxHeight");
			if(tag != null)
				collisionBoxHeight = tag.getData();
			
			float minX = x - (collisionBoxWidth * scale) / 2f;
			float minY = y;
			float minZ = z - (collisionBoxWidth * scale) / 2f;
			float maxX = x + (collisionBoxWidth * scale) / 2f;
			float maxY = y + (collisionBoxHeight * scale);
			float maxZ = z + (collisionBoxWidth * scale) / 2f;
			
			int minBlockX = (int) Math.floor(minX);
			int minBlockY = (int) Math.floor(minY);
			int minBlockZ = (int) Math.floor(minZ);
			int maxBlockX = (int) Math.floor(maxX);
			int maxBlockY = (int) Math.floor(maxY);
			int maxBlockZ = (int) Math.floor(maxZ);
			
			for(int y = minBlockY; y <= maxBlockY; ++y) {
				for(int z = minBlockZ; z <= maxBlockZ; ++z) {
					for(int x = minBlockX; x <= maxBlockX; ++x) {
						int blockId = MCWorldExporter.getApp().getWorld().getBlockId(x, y, z);
						Block block = BlockRegistry.getBlock(blockId);
						if(block.isWaterlogged() || block.getName().equals("minecraft:water"))
							return new MolangValue(true);
					}
				}
			}
			
			return new MolangValue(false);
		}
	}
	private class IsInLava extends MolangScript{
		public IsInLava() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int blockX = (int) Math.floor(x);
			int blockY = (int) Math.floor(y);
			int blockZ = (int) Math.floor(z);
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ);
			Block block = BlockRegistry.getBlock(blockId);
			return new MolangValue(block.getName().equals("minecraft:lava"));
		}
	}
	private class IsInWater extends MolangScript{
		public IsInWater() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int blockX = (int) Math.floor(x);
			int blockY = (int) Math.floor(y);
			int blockZ = (int) Math.floor(z);
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ);
			Block block = BlockRegistry.getBlock(blockId);
			return new MolangValue(block.isWaterlogged() || block.getName().equals("minecraft:water"));
		}
	}
	private class IsInWaterOrRain extends MolangScript{
		public IsInWaterOrRain() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			int blockX = (int) Math.floor(x);
			int blockY = (int) Math.floor(y);
			int blockZ = (int) Math.floor(z);
			int blockId = MCWorldExporter.getApp().getWorld().getBlockId(blockX, blockY, blockZ);
			Block block = BlockRegistry.getBlock(blockId);
			return new MolangValue(block.isWaterlogged() || block.getName().equals("minecraft:water"));
		}
	}
	private class IsJumping extends MolangScript{
		public IsJumping() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("IsJumping");
			if(tag != null)
				return new MolangValue(tag.getData() != 0);
			return new MolangValue(false);
		}
	}
	private class IsLayingDown extends MolangScript{
		public IsLayingDown() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("IsLayingDown");
			if(tag != null)
				return new MolangValue(tag.getData() != 0);
			return new MolangValue(false);
		}
	}
	private class IsLevitating extends MolangScript{
		public IsLevitating() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("OnGround");
			if(tag != null)
				return new MolangValue(tag.getData() == 0);
			return new MolangValue(false);
		}
	}
	private class IsStackable extends MolangScript{
		public IsStackable() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			NbtTagLong tag = (NbtTagLong) properties.get("IsStackable");
			if(tag != null)
				return new MolangValue(tag.getData() != 0);
			return new MolangValue(false);
		}
	}
	private class CameraRotation extends MolangScript{
		public CameraRotation() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dx = x - MCWorldExporter.getApp().getExportBounds().getOffsetX();
			float dy = y - MCWorldExporter.getApp().getExportBounds().getOffsetY();
			float dz = z - MCWorldExporter.getApp().getExportBounds().getOffsetZ();
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			dx /= length;
			dy /= length;
			dz /= length;
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float pitch = (float) Math.toDegrees(Math.asin(dy));
			
			int axis = (int) context.getTempDict().getField("arg_0").asNumber(context);
			if(axis <= 0)
				return new MolangValue(pitch);
			return new MolangValue(yaw);
		}
	}
	private class RotationToCamera extends MolangScript{
		public RotationToCamera() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float dx = MCWorldExporter.getApp().getExportBounds().getOffsetX() - x;
			float dy = MCWorldExporter.getApp().getExportBounds().getOffsetY() - y;
			float dz = MCWorldExporter.getApp().getExportBounds().getOffsetZ() - z;
			float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			dx /= length;
			dy /= length;
			dz /= length;
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float pitch = (float) Math.toDegrees(Math.asin(dy));
			
			int axis = (int) context.getTempDict().getField("arg_0").asNumber(context);
			if(axis <= 0)
				return new MolangValue(pitch);
			return new MolangValue(yaw);
		}
	}
	private class AnimTime extends MolangScript{
		public AnimTime() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info == null)
				return new MolangValue(0f);
			return new MolangValue(info.animTime);
		}
	}
	private class LifeTime extends MolangScript{
		public LifeTime() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info == null)
				return new MolangValue(0f);
			return new MolangValue(info.globalTime);
		}
	}
	private class AllAnimationsFinished extends MolangScript{
		public AllAnimationsFinished() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info == null)
				return new MolangValue(false);
			return new MolangValue(info.allAnimationsFinished);
		}
	}
	private class AnyAnimationFinished extends MolangScript{
		public AnyAnimationFinished() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info == null)
				return new MolangValue(false);
			return new MolangValue(info.anyAnimationFinished);
		}
	}
	private class KeyframeLerpTime extends MolangScript{
		public KeyframeLerpTime() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			AnimationInfo info = context.getAnimationInfo();
			if(info == null)
				return new MolangValue(0f);
			return new MolangValue(info.keyframeLerpTime);
		}
	}
	private class BoneAABB extends MolangScript{
		public BoneAABB() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			Map<String, MolangValue> minValues = new HashMap<String, MolangValue>();
			minValues.put("x", new MolangValue(-1f));
			minValues.put("y", new MolangValue(-1f));
			minValues.put("z", new MolangValue(-1f));
			Map<String, MolangValue> maxValues = new HashMap<String, MolangValue>();
			maxValues.put("x", new MolangValue(1f));
			maxValues.put("y", new MolangValue(1f));
			maxValues.put("z", new MolangValue(1f));
			Map<String, MolangValue> values = new HashMap<String, MolangValue>();
			values.put("min", new MolangValue(minValues));
			values.put("max", new MolangValue(maxValues));
			return new MolangValue(values);
		}
	}
	private class BoneOrientationTRS extends MolangScript{
		public BoneOrientationTRS() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String boneName = context.getTempDict().getField("arg_0").asString(context);
			float[] data = new float[] { 0f, 0f, 0f,   0f, 0f, 0f,   1f, 1f, 1f };
			AnimationInfo info = context.getAnimationInfo();
			if(info != null)
				data = info.getBoneOrientationTRS(boneName);
			Map<String, MolangValue> posValues = new HashMap<String, MolangValue>();
			posValues.put("x", new MolangValue(data[0]));
			posValues.put("y", new MolangValue(data[1]));
			posValues.put("z", new MolangValue(data[2]));
			Map<String, MolangValue> rotValues = new HashMap<String, MolangValue>();
			rotValues.put("x", new MolangValue(data[3]));
			rotValues.put("y", new MolangValue(data[4]));
			rotValues.put("z", new MolangValue(data[5]));
			Map<String, MolangValue> scaleValues = new HashMap<String, MolangValue>();
			scaleValues.put("x", new MolangValue(data[6]));
			scaleValues.put("y", new MolangValue(data[7]));
			scaleValues.put("z", new MolangValue(data[8]));
			Map<String, MolangValue> values = new HashMap<String, MolangValue>();
			values.put("t", new MolangValue(posValues));
			values.put("r", new MolangValue(rotValues));
			values.put("s", new MolangValue(scaleValues));
			return new MolangValue(values);
		}
	}
	private class BoneOrigin extends MolangScript{
		public BoneOrigin() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String boneName = context.getTempDict().getField("arg_0").asString(context);
			float[] data = new float[] { 0f, 0f, 0f};
			AnimationInfo info = context.getAnimationInfo();
			if(info != null)
				data = info.getBoneOrigin(boneName);
			Map<String, MolangValue> posValues = new HashMap<String, MolangValue>();
			posValues.put("x", new MolangValue(data[0]));
			posValues.put("y", new MolangValue(data[1]));
			posValues.put("z", new MolangValue(data[2]));
			return new MolangValue(posValues);
		}
	}
	private class BoneRotation extends MolangScript{
		public BoneRotation() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String boneName = context.getTempDict().getField("arg_0").asString(context);
			float[] data = new float[] { 0f, 0f, 0f};
			AnimationInfo info = context.getAnimationInfo();
			if(info != null)
				data = info.getBoneRotation(boneName);
			Map<String, MolangValue> rotValues = new HashMap<String, MolangValue>();
			rotValues.put("x", new MolangValue(data[0]));
			rotValues.put("y", new MolangValue(data[1]));
			rotValues.put("z", new MolangValue(data[2]));
			return new MolangValue(rotValues);
		}
	}
	private class GetDefaultBonePivot extends MolangScript{
		public GetDefaultBonePivot() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String boneName = context.getTempDict().getField("arg_0").asString(context);
			int axis = (int) context.getTempDict().getField("arg_1").asNumber(context);
			float[] data = new float[] { 0f, 0f, 0f};
			AnimationInfo info = context.getAnimationInfo();
			if(info != null)
				data = info.getBoneOrigin(boneName);
			if(axis <= 0)
				return new MolangValue(data[0]);
			else if(axis == 1)
				return new MolangValue(data[1]);
			else
				return new MolangValue(data[2]);
		}
	}
	private class GetLocatorOffset extends MolangScript{
		public GetLocatorOffset() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String locatorName = context.getTempDict().getField("arg_0").asString(context);
			int axis = (int) context.getTempDict().getField("arg_1").asNumber(context);
			Matrix matrix = new Matrix();
			AnimationInfo info = context.getAnimationInfo();
			if(info != null)
				matrix = info.getLocatorMatrix(locatorName);
			Vector3f pos = matrix.transformPoint(new Vector3f(0f, 0f, 0f));
			if(axis <= 0)
				return new MolangValue(pos.x);
			else if(axis == 1)
				return new MolangValue(pos.y);
			else
				return new MolangValue(pos.z);
		}
	}
	private class GetRootLocatorOffset extends MolangScript{
		public GetRootLocatorOffset() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			String locatorName = context.getTempDict().getField("arg_0").asString(context);
			int axis = (int) context.getTempDict().getField("arg_1").asNumber(context);
			Matrix matrix = new Matrix();
			AnimationInfo info = context.getAnimationInfo();
			if(info != null)
				matrix = info.getLocatorMatrix(locatorName);
			Vector3f pos = matrix.transformPoint(new Vector3f(0f, 0f, 0f));
			if(axis <= 0)
				return new MolangValue(pos.x);
			else if(axis == 1)
				return new MolangValue(pos.y);
			else
				return new MolangValue(pos.z);
		}
	}
	private class StandingScale extends MolangScript{
		public StandingScale() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(0f);
		}
	}
	
	public MolangQuery(String resourceId, NbtTagCompound properties, float x, float y, float z) {
		super();
		
		this.resourceId = resourceId;
		this.properties = properties;
		this.x = x;
		this.y = y;
		this.z = z;
		
		getFields().put("above_top_solid", new MolangValue(new MolangFunction(new AboveTopSolid())));
		getFields().put("all", new MolangValue(new MolangFunction(new All())));
		getFields().put("all_tags", new MolangValue(new MolangFunction(new AllTags())));
		getFields().put("any", new MolangValue(new MolangFunction(new Any())));
		getFields().put("any_tag", new MolangValue(new MolangFunction(new AnyTags())));
		getFields().put("has_any_family", new MolangValue(new MolangFunction(new HasAnyFamily())));
		getFields().put("approx_eq", new MolangValue(new MolangFunction(new ApproxEq())));
		getFields().put("block_has_all_tags", new MolangValue(new MolangFunction(new BlockHasAllTags())));
		getFields().put("block_has_any_tags", new MolangValue(new MolangFunction(new BlockHasAnyTags())));
		getFields().put("block_neighbor_has_all_tags", new MolangValue(new MolangFunction(new BlockNeighbourHasAllTags())));
		getFields().put("block_neighbor_has_any_tags", new MolangValue(new MolangFunction(new BlockNeighbourHasAnyTags())));
		getFields().put("block_property", new MolangValue(new MolangFunction(new BlockProperty())));
		getFields().put("block_state", new MolangValue(new MolangFunction(new BlockProperty())));
		getFields().put("count", new MolangValue(new MolangFunction(new Count())));
		getFields().put("debug_output", new MolangValue(new MolangFunction(new Log())));
		getFields().put("get_name", new MolangValue(new MolangFunction(new GetName())));
		getFields().put("has_biome_tag", new MolangValue(new MolangFunction(new HasBiomeTag())));
		getFields().put("has_biome", new MolangValue(new MolangFunction(new HasBiome())));
		getFields().put("has_block_property", new MolangValue(new MolangFunction(new HasBlockProperty())));
		getFields().put("has_block_state", new MolangValue(new MolangFunction(new HasBlockProperty())));
		getFields().put("get_block_name", new MolangValue(new MolangFunction(new GetBlockName())));
		getFields().put("has_property", new MolangValue(new MolangFunction(new HasBlockProperty())));
		getFields().put("heightmap", new MolangValue(new MolangFunction(new Heightmap())));
		getFields().put("in_range", new MolangValue(new MolangFunction(new InRange())));
		getFields().put("log", new MolangValue(new MolangFunction(new Log())));
		getFields().put("position", new MolangValue(new MolangFunction(new Position())));
		getFields().put("property", new MolangValue(new MolangFunction(new BlockProperty())));
		getFields().put("relative_block_has_all_tags", new MolangValue(new MolangFunction(new BlockNeighbourHasAllTags())));
		getFields().put("relative_block_has_any_tags", new MolangValue(new MolangFunction(new BlockNeighbourHasAnyTags())));
		getFields().put("get_equipped_item_name", new MolangValue(new MolangFunction(new GetEquippedItemName())));
		getFields().put("is_name_any", new MolangValue(new MolangFunction(new IsNameAny())));
		getFields().put("is_item_name_any", new MolangValue(new MolangFunction(new IsItemNameAny())));
		getFields().put("is_eating", new MolangValue(new MolangFunction(new IsEating())));
		getFields().put("is_roaring", new MolangValue(new MolangFunction(new IsRoaring())));
		getFields().put("head_x_rotation", new MolangValue(new MolangFunction(new HeadPitch())));
		getFields().put("head_y_rotation", new MolangValue(new MolangFunction(new HeadYaw())));
		getFields().put("target_x_rotation", new MolangValue(new MolangFunction(new HeadPitch())));
		getFields().put("target_y_rotation", new MolangValue(new MolangFunction(new HeadYaw())));
		getFields().put("time_of_day", new MolangValue(new MolangFunction(new TimeOfDay())));
		getFields().put("movement_direction", new MolangValue(new MolangFunction(new MovementDirection())));
		getFields().put("anger_level", new MolangValue(new MolangFunction(new AngerLevel())));
		getFields().put("average_frame_time", new MolangValue(new MolangFunction(new AverageFrameTime())));
		getFields().put("body_x_rotation", new MolangValue(new MolangFunction(new Pitch())));
		getFields().put("body_y_rotation", new MolangValue(new MolangFunction(new Yaw())));
		getFields().put("cape_flap_amount", new MolangValue(new MolangFunction(new CapeFlapAmount())));
		getFields().put("cardinal_facing", new MolangValue(new MolangFunction(new CardinalFacing())));
		getFields().put("cardinal_facing_2d", new MolangValue(new MolangFunction(new CardinalFacing2D())));
		getFields().put("day", new MolangValue(new MolangFunction(new Day())));
		getFields().put("death_ticks", new MolangValue(new MolangFunction(new DeathTicks())));
		getFields().put("delta_time", new MolangValue(new MolangFunction(new AverageFrameTime())));
		getFields().put("distance_from_camera", new MolangValue(new MolangFunction(new DistanceFromCamera())));
		getFields().put("frame_alpha", new MolangValue(new MolangFunction(new FrameAlpha())));
		getFields().put("had_component_group", new MolangValue(new MolangFunction(new HadComponentGroup())));
		getFields().put("has_component", new MolangValue(new MolangFunction(new HasComponent())));
		getFields().put("has_armor_slot", new MolangValue(new MolangFunction(new HasArmorSlot())));
		getFields().put("health", new MolangValue(new MolangFunction(new Health())));
		getFields().put("is_alive", new MolangValue(new MolangFunction(new IsAlive())));
		getFields().put("is_angry", new MolangValue(new MolangFunction(new IsAngry())));
		getFields().put("is_baby", new MolangValue(new MolangFunction(new IsBaby())));
		getFields().put("is_chested", new MolangValue(new MolangFunction(new IsChested())));
		getFields().put("is_gliding", new MolangValue(new MolangFunction(new IsGliding())));
		getFields().put("is_illager_captain", new MolangValue(new MolangFunction(new IsIllagerCaptain())));
		getFields().put("is_item_equipped", new MolangValue(new MolangFunction(new IsItemEquipped())));
		getFields().put("is_moving", new MolangValue(new MolangFunction(new IsMoving())));
		getFields().put("is_on_fire", new MolangValue(new MolangFunction(new IsOnFire())));
		getFields().put("is_on_ground", new MolangValue(new MolangFunction(new IsOnGround())));
		getFields().put("is_on_screen", new MolangValue(new MolangFunction(new IsOnScreen())));
		getFields().put("is_onfire", new MolangValue(new MolangFunction(new IsOnFire())));
		getFields().put("is_orphaned", new MolangValue(new MolangFunction(new IsOrphaned())));
		getFields().put("is_saddled", new MolangValue(new MolangFunction(new IsSaddled())));
		getFields().put("is_scared", new MolangValue(new MolangFunction(new IsScared())));
		getFields().put("is_sheared", new MolangValue(new MolangFunction(new IsSheared())));
		getFields().put("is_sitting", new MolangValue(new MolangFunction(new IsSitting())));
		getFields().put("is_stunned", new MolangValue(new MolangFunction(new IsStunned())));
		getFields().put("is_swimming", new MolangValue(new MolangFunction(new IsSwimming())));
		getFields().put("is_tamed", new MolangValue(new MolangFunction(new IsTamed())));
		getFields().put("last_frame_time", new MolangValue(new MolangFunction(new AverageFrameTime())));
		getFields().put("lod_index", new MolangValue(new MolangFunction(new LodIndex())));
		getFields().put("mark_variant", new MolangValue(new MolangFunction(new MarkVariant())));
		getFields().put("max_health", new MolangValue(new MolangFunction(new MaxHealth())));
		getFields().put("maximum_frame_time", new MolangValue(new MolangFunction(new AverageFrameTime())));
		getFields().put("minimum_frame_time", new MolangValue(new MolangFunction(new AverageFrameTime())));
		getFields().put("model_scale", new MolangValue(new MolangFunction(new ModelScale())));
		getFields().put("modified_move_speed", new MolangValue(new MolangFunction(new ModifiedMoveSpeed())));
		getFields().put("moon_brightness", new MolangValue(new MolangFunction(new MoonBrightness())));
		getFields().put("moon_phase", new MolangValue(new MolangFunction(new MoonPhase())));
		getFields().put("on_fire_time", new MolangValue(new MolangFunction(new OnFireTime())));
		getFields().put("out_of_control", new MolangValue(new MolangFunction(new OutOfControl())));
		getFields().put("position_delta", new MolangValue(new MolangFunction(new PositionDelta())));
		getFields().put("show_bottom", new MolangValue(new MolangFunction(new ShowBottom())));
		getFields().put("skin_id", new MolangValue(new MolangFunction(new SkinId())));
		getFields().put("variant", new MolangValue(new MolangFunction(new Variant())));
		getFields().put("vertical_speed", new MolangValue(new MolangFunction(new VerticalSpeed())));
		getFields().put("yaw_speed", new MolangValue(new MolangFunction(new YawSpeed())));
		getFields().put("can_climb", new MolangValue(new MolangFunction(new CanClimb())));
		getFields().put("can_fly", new MolangValue(new MolangFunction(new CanFly())));
		getFields().put("ground_speed", new MolangValue(new MolangFunction(new GroundSpeed())));
		getFields().put("has_collision", new MolangValue(new MolangFunction(new HasCollision())));
		getFields().put("has_gravity", new MolangValue(new MolangFunction(new HasGravity())));
		getFields().put("has_target", new MolangValue(new MolangFunction(new HasTarget())));
		getFields().put("is_charging", new MolangValue(new MolangFunction(new IsCharging())));
		getFields().put("is_croaking", new MolangValue(new MolangFunction(new IsCroaking())));
		getFields().put("is_ignited", new MolangValue(new MolangFunction(new IsIgnited())));
		getFields().put("is_in_contact_with_water", new MolangValue(new MolangFunction(new IsInContactWithWater())));
		getFields().put("is_in_lava", new MolangValue(new MolangFunction(new IsInLava())));
		getFields().put("is_in_water", new MolangValue(new MolangFunction(new IsInWater())));
		getFields().put("is_in_water_or_rain", new MolangValue(new MolangFunction(new IsInWaterOrRain())));
		getFields().put("is_jumping", new MolangValue(new MolangFunction(new IsJumping())));
		getFields().put("is_laying_down", new MolangValue(new MolangFunction(new IsLayingDown())));
		getFields().put("is_levitating", new MolangValue(new MolangFunction(new IsLevitating())));
		getFields().put("is_stackable", new MolangValue(new MolangFunction(new IsStackable())));
		getFields().put("modified_distance_moved", new MolangValue(new MolangFunction(new ModifiedDistanceMoved())));
		getFields().put("walk_distance", new MolangValue(new MolangFunction(new DistanceMoved())));
		getFields().put("camera_rotation", new MolangValue(new MolangFunction(new CameraRotation())));
		getFields().put("rotation_to_camera", new MolangValue(new MolangFunction(new RotationToCamera())));
		getFields().put("anim_time", new MolangValue(new MolangFunction(new AnimTime())));
		getFields().put("life_time", new MolangValue(new MolangFunction(new LifeTime())));
		getFields().put("all_animations_finished", new MolangValue(new MolangFunction(new AllAnimationsFinished())));
		getFields().put("any_animation_finished", new MolangValue(new MolangFunction(new AnyAnimationFinished())));
		getFields().put("bone_aabb", new MolangValue(new MolangFunction(new BoneAABB())));
		//getFields().put("bone_orientation_matrix", new MolangValue(new MolangFunction(new BoneOrientationMatrix())));
		getFields().put("bone_orientation_trs", new MolangValue(new MolangFunction(new BoneOrientationTRS())));
		getFields().put("bone_origin", new MolangValue(new MolangFunction(new BoneOrigin())));
		getFields().put("bone_rotation", new MolangValue(new MolangFunction(new BoneRotation())));
		getFields().put("get_default_bone_pivot", new MolangValue(new MolangFunction(new GetDefaultBonePivot())));
		getFields().put("get_locator_offset", new MolangValue(new MolangFunction(new GetLocatorOffset())));
		getFields().put("get_root_locator_offset", new MolangValue(new MolangFunction(new GetRootLocatorOffset())));
		getFields().put("key_frame_lerp_time", new MolangValue(new MolangFunction(new KeyframeLerpTime())));
		getFields().put("standing_scale", new MolangValue(new MolangFunction(new StandingScale())));
	}
	
}
