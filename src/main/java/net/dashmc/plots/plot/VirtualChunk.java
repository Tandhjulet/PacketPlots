package net.dashmc.plots.plot;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import lombok.Getter;
import net.dashmc.plots.data.IDataHolder;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;

public class VirtualChunk implements IDataHolder {
	private @Getter ChunkCoordIntPair coordPair;

	public VirtualChunk(ChunkCoordIntPair coordPair) {
		this.coordPair = coordPair;
	}

	public VirtualChunk(DataInputStream stream) {
		deserialize(stream);
	}

	@Override
	public void deserialize(DataInputStream stream) {

	}

	@Override
	public void serialize(DataOutputStream stream) {

	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof VirtualChunk))
			return false;
		VirtualChunk chunk = (VirtualChunk) other;
		return chunk.getCoordPair().equals(getCoordPair());
	}

}
