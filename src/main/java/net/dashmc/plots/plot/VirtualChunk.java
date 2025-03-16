package net.dashmc.plots.plot;

import lombok.Getter;
import net.dashmc.plots.data.BufferedDataStream;
import net.dashmc.plots.data.IDataHolder;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;

public class VirtualChunk implements IDataHolder {
	private @Getter ChunkCoordIntPair coordPair;

	public VirtualChunk(ChunkCoordIntPair coordPair) {
		this.coordPair = coordPair;
	}

	public VirtualChunk(BufferedDataStream stream) {
		serialize(stream);
	}

	@Override
	public void deserialize(BufferedDataStream stream) {

	}

	@Override
	public void serialize(BufferedDataStream stream) {

	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VirtualChunk))
			return false;
		VirtualChunk chunk = (VirtualChunk) other;
		return chunk.getCoordPair().equals(getCoordPair());
	}

}
