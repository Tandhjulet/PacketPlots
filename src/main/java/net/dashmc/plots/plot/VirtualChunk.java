package net.dashmc.plots.plot;

import lombok.Getter;
import net.dashmc.plots.data.BufferedDataStream;
import net.dashmc.plots.data.DataHolder;

public class VirtualChunk extends DataHolder {
	private @Getter int x, z;

	public VirtualChunk(BufferedDataStream stream) {
		super(stream);
	}

	@Override
	public void deserialize(BufferedDataStream stream) {

	}

	@Override
	public void serialize(BufferedDataStream stream) {

	}

}
