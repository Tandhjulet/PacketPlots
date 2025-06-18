package net.dashmc.plots.plot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;

import com.google.common.collect.Lists;

import lombok.Getter;
import net.dashmc.plots.data.IDataHolder;
import net.dashmc.plots.utils.Debug;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockContainer;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IContainer;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.World;
import net.minecraft.server.v1_8_R3.Chunk.EnumTileEntityState;
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
	private static Field xCoordField;
	private static Field zCoordField;
	private static Field chunkMapField;

	private static byte[][] lightArrays = new byte[16][];
	private static Field nonEmptyBlockCountField;

	private @Getter ChunkCoordIntPair coordPair;

	private Chunk chunk;
	private @Getter VirtualEnvironment environment;
	private World world;
	private Section[] sections = new Section[16];
	private Map<BlockPosition, TileEntity> tileEntities = new HashMap<>();
	private char sectionMask = 0;
	private char allowedSections;

	public VirtualChunk(VirtualEnvironment environment, ChunkCoordIntPair coordPair, World world,
			char allowedSections) {
		this.environment = environment;
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

	public VirtualChunk(VirtualEnvironment environment, World world, DataInputStream stream) throws IOException {
		this.environment = environment;
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
			}
		}
	}

	public boolean isSectionSet(byte sectionIndex) {
		return (sectionMask & (1 << sectionIndex)) > 0;
	}

	public Chunk getChunk() {
		if (this.chunk != null)
			return this.chunk;
		// try to load it again if it is not loaded!
		this.chunk = world.getChunkAt(coordPair.x, coordPair.z);
		return this.chunk;
	}

	public boolean setBlock(BlockPosition pos, IBlockData blockData) {
		byte relX = (byte) (pos.getX() & 15);
		byte relZ = (byte) (pos.getZ() & 15);
		byte yPos = (byte) (pos.getY() >> 4);

		Debug.log("Checking if coordinate set is inside chunk: " + (pos.getX() >> 4) + " != " + coordPair.x + " || "
				+ (pos.getZ() >> 4) + " != " + coordPair.z);
		if (pos.getX() >> 4 != coordPair.x || pos.getZ() >> 4 != coordPair.z) {
			Bukkit.getLogger().warning(
					"setBlock called on VirtualChunk even though the reference position was not in the virtualized chunk.");
			return false;
		} else if ((allowedSections & 1 << yPos) == 0) {
			Bukkit.getLogger().warning(
					"setBlock called on VirtualChunk even though the reference position was not in the virtualized section.");
			return false;
		}

		IBlockData currentBlock = getBlockData(pos);
		if (currentBlock == blockData)
			return false;

		Block block1 = currentBlock.getBlock();
		Block block = blockData.getBlock();
		Section section = sections[yPos];
		if (section == null) {
			if (block == Blocks.AIR)
				return false;

			ChunkSection chunkSection = getChunk().getSections()[yPos];
			if (chunkSection == null)
				chunkSection = new ChunkSection(yPos, !world.worldProvider.o());
			section = new Section(chunkSection);
			sectionMask |= 1 << yPos;
		}
		section.setBlock(relX, (byte) (pos.getY() & 15), relZ, blockData);

		if (block1 != block) {
			if (!this.world.isClientSide) {
				// block1.remove(this.world, pos, currentBlock);
			}
		}

		if (section.getBlock(relX, (byte) (pos.getY() & 15), relZ) != block)
			return false;

		if (block1 instanceof IContainer) {
			TileEntity tileEntity = getTileEntity(pos, EnumTileEntityState.CHECK);
			if (tileEntity != null)
				tileEntity.E();
		}

		if (block1 != block && block instanceof BlockContainer) {
			VirtualBlock.onPlace(environment, pos, blockData);
		}

		if (block instanceof IContainer) {
			TileEntity tileEntity = getTileEntity(pos, EnumTileEntityState.CHECK);
			if (tileEntity == null) {
				tileEntity = ((IContainer) block).a(world, block.toLegacyData(blockData));
				environment.setTileEntity(pos, tileEntity);
			}

			if (tileEntity != null)
				tileEntity.E();
		}

		return true;
	}

	public void setTileEntity(BlockPosition pos, TileEntity entity) {
		entity.a(pos);
		if (getBlockData(pos).getBlock() instanceof IContainer) {
			if (tileEntities.containsKey(pos)) {
				tileEntities.get(pos).y();
			}

			entity.D();
			this.tileEntities.put(pos, entity);
		}
	}

	@Override
	public void deserialize(DataInputStream stream) throws IOException {
		this.coordPair = new ChunkCoordIntPair(stream.readInt(), stream.readInt());

		Debug.log("Deserializing chunk @ " + coordPair.x + " , " + coordPair.z);
		this.chunk = world.getChunkAt(coordPair.x, coordPair.z);

		this.sectionMask = stream.readChar();
		for (int i = 0; i < 16; i++) {
			if ((sectionMask & (1 << i)) == 0)
				continue;

			sections[i] = new Section(stream);
		}
	}

	@Override
	public void serialize(DataOutputStream stream) throws IOException {
		stream.writeInt(coordPair.x);
		stream.writeInt(coordPair.z);
		Debug.log("Serializing chunk @ " + coordPair.x + " , " + coordPair.z);

		stream.writeChar(sectionMask);
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

	public PacketPlayOutMapChunk getPacket(int mask, boolean isOverworld, boolean includeBiome) {
		ChunkMap chunkMap = getChunkMap(mask, isOverworld, includeBiome);
		PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk();

		try {
			xCoordField.set(packet, coordPair.x);
			zCoordField.set(packet, coordPair.z);
			chunkMapField.set(packet, chunkMap);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return packet;
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
				ChunkSection chunkSection = getChunk().getSections()[j];
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
			System.arraycopy(getChunk().getBiomeIndex(), 0, chunkMap.a, j, 256);
		}

		return chunkMap;
	}

	public TileEntity getTileEntity(BlockPosition pos, EnumTileEntityState entityState) {
		TileEntity tileEntity = tileEntities.get(pos);
		if (tileEntity == null) {
			if (entityState == EnumTileEntityState.IMMEDIATE) {
				Block block = this.getType(pos);

				tileEntity = block.isTileEntity() ? ((IContainer) block).a(this.world, getBlockAsLegacyData(pos))
						: null;
				environment.setTileEntity(pos, tileEntity);
			} else if (entityState == EnumTileEntityState.QUEUED)
				throw new NotImplementedException();
		} else if (tileEntity.x()) {
			this.tileEntities.remove(pos);
			return null;
		}

		return tileEntity;
	}

	public int getBlockAsLegacyData(BlockPosition blockposition) {
		IBlockData blockData = getBlockData(blockposition);
		return blockData.getBlock().toLegacyData(blockData);
	}

	public Block getType(BlockPosition pos) {
		return getBlockData(pos).getBlock();
	}

	public IBlockData getBlockData(BlockPosition pos) {
		if (pos.getY() >= 0 && pos.getY() >> 4 < this.sections.length) {
			Section section = sections[pos.getY() >> 4];
			if (section != null) {
				byte x = (byte) (pos.getX() & 15);
				byte y = (byte) (pos.getY() & 15);
				byte z = (byte) (pos.getZ() & 15);
				return section.getType(x, y, z);
			}
		}
		return Blocks.AIR.getBlockData();
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
				yPos = (byte) (section.getYPosition() >> 4);
				blockIds = section.getIdArray().clone();
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

		public void setBlock(byte x, byte y, byte z, IBlockData blockData) {
			IBlockData curr = getType(x, y, z);
			Block currBlock = curr.getBlock();
			Block newBlock = blockData.getBlock();
			if (currBlock != Blocks.AIR)
				--this.nonEmptyBlockCount;
			if (newBlock != Blocks.AIR)
				++this.nonEmptyBlockCount;

			blockIds[y << 8 | z << 4 | x] = (char) Block.d.b(blockData);
		}
	}

	static {
		try {
			nonEmptyBlockCountField = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
			nonEmptyBlockCountField.setAccessible(true);

			xCoordField = PacketPlayOutMapChunk.class.getDeclaredField("a");
			xCoordField.setAccessible(true);
			zCoordField = PacketPlayOutMapChunk.class.getDeclaredField("b");
			zCoordField.setAccessible(true);
			chunkMapField = PacketPlayOutMapChunk.class.getDeclaredField("c");
			chunkMapField.setAccessible(true);

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
