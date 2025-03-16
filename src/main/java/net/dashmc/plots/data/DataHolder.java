package net.dashmc.plots.data;

public abstract class DataHolder {
	public DataHolder(BufferedDataStream stream) {
		if (stream != null)
			deserialize(stream);
	}

	public abstract void deserialize(BufferedDataStream stream);

	public abstract void serialize(BufferedDataStream stream);
}
