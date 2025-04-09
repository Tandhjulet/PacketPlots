package net.dashmc.plots.plot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lombok.Getter;
import net.dashmc.plots.data.IDataHolder;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.IBlockData;

/*
 * Data structure:
 * int xCoord
 * int zCoord
 * 
 * byte amount of sections
 * array of sections
 */
public class VirtualChunk implements IDataHolder {
	private @Getter ChunkCoordIntPair coordPair;
	private Section[] sections = new Section[16];

	public VirtualChunk(ChunkCoordIntPair coordPair) {
		this.coordPair = coordPair;
	}

	public VirtualChunk(DataInputStream stream) throws IOException {
		deserialize(stream);
	}

	@Override
	public void deserialize(DataInputStream stream) throws IOException {
		this.coordPair = new ChunkCoordIntPair(stream.readInt(), stream.readInt());

		byte length = stream.readByte();
		for (int i = 0; i < length; i++) {
			sections[i] = new Section(stream);
		}
	}

	@Override
	public void serialize(DataOutputStream stream) throws IOException {
		stream.writeInt(coordPair.x);
		stream.writeInt(coordPair.z);

		stream.writeByte(sections.length);
		for (Section virtualChunkSection : sections) {
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

	/*
	 * Data structure:
	 * yPos (1 byte)
	 * nonEmptyBlockCount (2 bytes)
	 * 
	 * array with length nonEmptyBlockCount of:
	 * - index (2 bytes)
	 * - blockId (2 bytes)
	 */
	public class Section implements IDataHolder {
		private @Getter byte yPos;
		private char nonEmptyBlockCount;
		// Diff is kept here - not all of the blocks in the chunk
		private char[] blockIds = new char[4096];

		public Section(DataInputStream stream) throws IOException {
			deserialize(stream);
		}

		@Override
		public void deserialize(DataInputStream stream) throws IOException {
			this.yPos = stream.readByte();
			this.nonEmptyBlockCount = stream.readChar();
			for (short i = 0; i < nonEmptyBlockCount; i++) {
				char pos = stream.readChar();
				char blockId = stream.readChar();
				blockIds[pos] = blockId;
			}
		}

		@Override
		public void serialize(DataOutputStream stream) throws IOException {
			stream.writeByte(yPos);
			stream.writeChar(nonEmptyBlockCount);
			for (char i = 0; i < blockIds.length; i++) {
				// Don't serialize air blocks - hopefully this can be made up
				// by the extra char it takes to write the index...
				if (blockIds[i] == 0)
					continue;

				stream.writeChar(i);
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

	}

}
