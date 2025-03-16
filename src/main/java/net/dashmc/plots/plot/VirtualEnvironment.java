package net.dashmc.plots.plot;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import lombok.Getter;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.data.BufferedDataStream;
import net.dashmc.plots.data.IDataHolder;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;

/*
 * Data structure:
 * player uuid (16 bytes - 2x long)
 * world uid (16 bytes - 2x long)
 * 
 * amount of virtual chunks (4 bytes - int)
 * array of VirtualChunk
 */

public class VirtualEnvironment implements IDataHolder {
	private @Getter VirtualChunk[] virtualChunks;
	private @Getter World world;
	private @Getter UUID ownerUuid;

	public VirtualEnvironment(BufferedDataStream stream) {
		serialize(stream);
	}

	public Player getOwner() {
		return Bukkit.getPlayer(ownerUuid);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deserialize(BufferedDataStream stream) {
		this.ownerUuid = stream.readUUID();
		this.world = Bukkit.getWorld(stream.readUUID());

		int arraySize = stream.readInt();

		int cP = 0;
		HashSet<ChunkCoordIntPair> chunkCoordPairs = (HashSet<ChunkCoordIntPair>) PacketPlots.getPlotConfig()
				.getVirtualChunks().clone();
		this.virtualChunks = new VirtualChunk[chunkCoordPairs.size()];

		// Read in the saved chunks. If any of them aren't specified as virtual in the
		// config any more discard them:
		for (int i = 0; i < arraySize; i++) {
			VirtualChunk chunk = new VirtualChunk(stream);
			if (!chunkCoordPairs.contains(chunk.getCoordPair()))
				continue;

			chunkCoordPairs.remove(chunk.getCoordPair());
			this.virtualChunks[cP++] = chunk;
		}

		// Fill the remaining spots, if any, with new virtual chunks
		for (ChunkCoordIntPair pair : chunkCoordPairs) {
			this.virtualChunks[cP++] = new VirtualChunk(pair);
		}

	}

	@Override
	public void serialize(BufferedDataStream stream) {
		stream.writeUUID(ownerUuid);
		stream.writeUUID(world.getUID());

		stream.writeInt(virtualChunks.length);
		for (VirtualChunk virtualChunk : virtualChunks) {
			virtualChunk.serialize(stream);
		}

	}

}
