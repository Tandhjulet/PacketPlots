package net.dashmc.plots.data;

public interface IDataHolder {
	public abstract void deserialize(BufferedDataStream stream);

	public abstract void serialize(BufferedDataStream stream);
}
