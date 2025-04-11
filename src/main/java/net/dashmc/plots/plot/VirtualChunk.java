package net.dashmc.plots.plot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;

import com.google.common.collect.Lists;

import lombok.Getter;
import net.dashmc.plots.data.IDataHolder;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.World;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;

/*
 * Data structure:
 * int xCoord
 * int zCoord
 * 
 * char section bit mask
 * array of sections
 */
public class VirtualChunk implements IDataHolder {
	private static byte[][] lightArrays = new byte[16][];
	private static Field nonEmptyBlockCountField;

	private @Getter ChunkCoordIntPair coordPair;

	private Chunk chunk;
	private World world;
	private Section[] sections = new Section[16];
	private char sectionMask = 0;
	private char allowedSections;

	public VirtualChunk(ChunkCoordIntPair coordPair, World world, char allowedSections) {
		this.coordPair = coordPair;
		this.world = world;
		this.chunk = world.getChunkAt(coordPair.x, coordPair.z);
		this.allowedSections = allowedSections;

		int i = 0;
		for (ChunkSection section : chunk.getSections()) {
			if (section != null && (allowedSections & 1 << i) != 0) {
				sectionMask |= 1 << i;
				sections[i] = new Section(section);
			}

			i++;
		}
	}

	public VirtualChunk(World world, DataInputStream stream) throws IOException {
		this.world = world;
		deserialize(stream);
	}

	public void setAllowedSections(char allowedSections) {
		this.allowedSections = allowedSections;
		for (int i = 0; i < 16; i++) {
			int mask = 1 << i;
			// if there is a section which isnt allowed, remove it
			if ((sectionMask & mask) != 0 && (allowedSections & mask) == 0) {
				int removeMask = 0xffff ^ (1 << i);
				sectionMask &= removeMask;

				sections[i] = null;
				Bukkit.getLogger().info("removed section " + i);
			}
		}
	}

	@Override
	public void deserialize(DataInputStream stream) throws IOException {
		this.coordPair = new ChunkCoordIntPair(stream.readInt(), stream.readInt());
		this.chunk = world.getChunkAt(coordPair.x, coordPair.z);

		this.sectionMask = stream.readChar();
		for (int i = 0; i < 16; i++) {
			if ((sectionMask & (1 << i)) == 0)
				continue;

			sections[i] = new Section(stream);
			Bukkit.getLogger().info("deserialized " + i);
		}
	}

	@Override
	public void serialize(DataOutputStream stream) throws IOException {
		stream.writeInt(coordPair.x);
		stream.writeInt(coordPair.z);

		stream.writeChar(sectionMask);
		Bukkit.getLogger().info("serialized " + sectionMask);
		for (Section virtualChunkSection : sections) {
			if (virtualChunkSection == null)
				continue;

			virtualChunkSection.serialize(stream);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VirtualChunk))
			return false;
		VirtualChunk chunk = (VirtualChunk) other;
		return chunk.getCoordPair().equals(getCoordPair());
	}

	public ChunkMap getChunkMap(int mask, boolean isOverworld, boolean includeBiome) {
		ChunkMap chunkMap = new ChunkMap();
		List<Section> arraylist = Lists.newArrayList();

		int j;
		for (j = 0; j < sections.length; j++) {
			if ((allowedSections & 1 << j) != 0) {
				Section section = sections[j];
				if (section != null && !section.isEmpty() && (mask & 1 << j) != 0) {
					chunkMap.b |= 1 << j;
					arraylist.add(section);
				}
			} else {
				ChunkSection chunkSection = chunk.getSections()[j];
				if (chunkSection == null)
					continue;
				Section section = new Section(chunkSection);
				if (!section.isEmpty() && (mask & 1 << j) != 0) {
					chunkMap.b |= 1 << j;
					arraylist.add(section);
				}
			}
		}

		int nonEmptyChunkSections = Integer.bitCount(chunkMap.b);
		chunkMap.a = new byte[calculateNeededBytes(nonEmptyChunkSections, isOverworld, includeBiome)];

		j = 0;
		Section section;
		Iterator<Section> iterator = arraylist.iterator();
		while (iterator.hasNext()) {
			section = iterator.next();
			char[] idArray = section.getBlockIds();

			for (int l = 0; l < idArray.length; l++) {
				char c0 = idArray[l];
				chunkMap.a[j++] = (byte) (c0 & 255);
				chunkMap.a[j++] = (byte) (c0 >> 8 & 255);
			}
		}

		byte[] lightArray = lightArrays[nonEmptyChunkSections];
		System.arraycopy(lightArray, 0, chunkMap.a, j, lightArray.length);

		if (isOverworld) {
			j += lightArray.length;
			System.arraycopy(lightArray, 0, chunkMap.a, j, lightArray.length);
		}

		if (includeBiome) {
			j += lightArray.length;
			System.arraycopy(chunk.getBiomeIndex(), 0, chunkMap.a, j, 256);
		}

		return chunkMap;
	}

	private static int calculateNeededBytes(int i, boolean isOverworld, boolean includeBiome) {
		int j = i * 2 * 16 * 16 * 16;
		int k = i * 16 * 16 * 8;
		int l = isOverworld ? i * 16 * 16 * 8 : 0;
		int biome = includeBiome ? 256 : 0;

		return j + k + l + biome;
	}

	/*
	 * Data structure:
	 * yPos (1 byte)
	 * nonEmptyBlockCount (2 bytes)
	 * 
	 * array with length 4096:
	 * - blockId (2 bytes)
	 */
	public class Section implements IDataHolder {
		private @Getter byte yPos;
		private char nonEmptyBlockCount;
		private @Getter char[] blockIds = new char[4096];

		public Section(ChunkSection section) {
			try {
				yPos = (byte) section.getYPosition();
				blockIds = section.getIdArray();
				nonEmptyBlockCount = (char) nonEmptyBlockCountField.getInt(section);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		public Section(DataInputStream stream) throws IOException {
			deserialize(stream);
		}

		@Override
		public void deserialize(DataInputStream stream) throws IOException {
			this.yPos = stream.readByte();
			this.nonEmptyBlockCount = stream.readChar();
			for (short i = 0; i < blockIds.length; i++) {
				char blockId = stream.readChar();
				blockIds[i] = blockId;
			}
		}

		@Override
		public void serialize(DataOutputStream stream) throws IOException {
			stream.writeByte(yPos);
			stream.writeChar(nonEmptyBlockCount);
			for (char i = 0; i < blockIds.length; i++) {
				stream.writeChar(blockIds[i]);
			}
		}

		public IBlockData getType(byte x, byte y, byte z) {
			IBlockData ibd = Block.d.a(this.blockIds[y << 8 | z << 4 | x]);
			return ibd == null ? Blocks.AIR.getBlockData() : ibd;
		}

		public Block getBlock(byte x, byte y, byte z) {
			return getType(x, y, z).getBlock();
		}

		public boolean isEmpty() {
			return this.nonEmptyBlockCount == 0;
		}
	}

	static {
		try {
			nonEmptyBlockCountField = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
			nonEmptyBlockCountField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < lightArrays.length; i++) {
			int lightArrayLength = i * 16 * 16 * 8;
			lightArrays[i] = new byte[lightArrayLength];
			Arrays.fill(lightArrays[i], (byte) 255);
		}
	}
}
