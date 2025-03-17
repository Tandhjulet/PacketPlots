package net.dashmc.plots.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IDataHolder {
	public abstract void deserialize(DataInputStream stream) throws IOException;

	public abstract void serialize(DataOutputStream stream) throws IOException;
}
