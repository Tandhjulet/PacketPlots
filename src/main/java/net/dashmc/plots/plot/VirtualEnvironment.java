package net.dashmc.plots.plot;

import java.io.File;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.data.BufferedDataStream;
import net.dashmc.plots.data.IDataHolder;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListenerPlayIn;

/*
 * Data structure:
 * player uuid (16 bytes - 2x long)
 * world uid (16 bytes - 2x long)
 * 
 * amount of virtual chunks (4 bytes - int)
 * array of VirtualChunk
 */

public class VirtualEnvironment implements IDataHolder {
	private static final String NETTY_PIPELINE_NAME = "VirtualEnvironment";
	private static final File DATA_DIRECTORY = new File(PacketPlots.getInstance().getDataFolder(), "data");

	private @Getter VirtualChunk[] virtualChunks;
	private @Getter World world;
	private @Getter UUID ownerUuid;

	public VirtualEnvironment(Player player) {
		File dataFile = new File(DATA_DIRECTORY, player.getUniqueId() + ".dat");
		if (dataFile.exists()) {
			// create BufferedDataStream from the file and
			// pass it to the other constructor
			return;
		}

		// initialize empty virtual chunks etc. + serialize

		togglePacketHandler(false);
	}

	public VirtualEnvironment(BufferedDataStream stream) {
		deserialize(stream);

		togglePacketHandler(false);
	}

	private void togglePacketHandler(boolean remove) {
		Player owner = getOwner();
		if (owner == null)
			return;

		EntityPlayer entityPlayer = ((CraftPlayer) owner).getHandle();
		Channel channel = entityPlayer.playerConnection.networkManager.channel;
		if (channel.pipeline().get(NETTY_PIPELINE_NAME) == null && !remove) {
			channel.pipeline().addBefore("packet_handler", NETTY_PIPELINE_NAME, new PacketHandler());
		} else if (channel.pipeline().get(NETTY_PIPELINE_NAME) != null && remove) {
			channel.pipeline().remove(NETTY_PIPELINE_NAME);
		}
	}

	public Player getOwner() {
		return Bukkit.getPlayer(ownerUuid);
	}

	public void intercept(Packet<PacketListenerPlayIn> packet) {

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

	/**
	 * PacketHandler for this virtual environment. Sorts through the packets and
	 * passes the important ones to VirtualEnvironment#intercept.
	 */
	private class PacketHandler extends ChannelDuplexHandler {

		// Outgoing
		@Override
		public void write(ChannelHandlerContext chx, Object packet, ChannelPromise promise) throws Exception {
			super.write(chx, packet, promise);
		}

		// Incomming
		@Override
		public void channelRead(ChannelHandlerContext chx, Object packet) throws Exception {
			super.channelRead(chx, packet);
		}

	}

	static {
		DATA_DIRECTORY.mkdirs();
	}

}
