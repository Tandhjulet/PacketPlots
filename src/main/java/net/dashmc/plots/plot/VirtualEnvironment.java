package net.dashmc.plots.plot;

import java.io.FileNotFoundException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import lombok.Getter;
import net.dashmc.plots.data.BufferedDataStream;
import net.dashmc.plots.data.DataHolder;

/*
 * Data structure:
 * player uuid (16 bytes - 2x long)
 * world uid (16 bytes - 2x long)
 * 
 * amount of virtual chunks (4 bytes - int)
 * array of VirtualChunk
 */

public class VirtualEnvironment extends DataHolder {
	private @Getter VirtualChunk[] virtualChunks;
	private @Getter World world;
	private @Getter UUID uuid;

	public VirtualEnvironment(BufferedDataStream stream) throws FileNotFoundException {
		super(stream);
	}

	public Player getPlayer() {
		return Bukkit.getPlayer(uuid);
	}

	@Override
	public void deserialize(BufferedDataStream stream) {
		this.uuid = stream.readUUID();
		this.world = Bukkit.getWorld(stream.readUUID());

		int arraySize = stream.readInt();
		this.virtualChunks = new VirtualChunk[arraySize];
		for (int i = 0; i < arraySize; i++) {
			this.virtualChunks[i] = new VirtualChunk(stream);
		}
	}

	@Override
	public void serialize(BufferedDataStream stream) {
		stream.writeUUID(uuid);
		stream.writeUUID(world.getUID());

		stream.writeInt(virtualChunks.length);
		for (VirtualChunk virtualChunk : virtualChunks) {
			virtualChunk.serialize(stream);
		}

	}

}
