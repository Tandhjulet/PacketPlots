package net.dashmc.plots.plot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
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
	private static final HashMap<Player, VirtualEnvironment> virtualEnvironments = new HashMap<>();

	private static final String NETTY_PIPELINE_NAME = "VirtualEnvironment";
	private static final File DATA_DIRECTORY = new File(PacketPlots.getInstance().getDataFolder(), "data");

	private @Getter VirtualChunk[] virtualChunks;
	private @Getter World world;
	private @Getter UUID ownerUuid;

	public static VirtualEnvironment get(Player player) {
		return virtualEnvironments.get(player);
	}

	public VirtualEnvironment(Player player) throws IOException {
		if (player == null || !player.isOnline())
			throw new IOException(
					"Tried initialization of VirtualEnvironment for offline player: " + player.getUniqueId());

		virtualEnvironments.put(player, this);
		File dataFile = new File(DATA_DIRECTORY, player.getUniqueId() + ".dat");

		if (dataFile.exists()) {
			FileInputStream fileInputStream = new FileInputStream(dataFile);
			DataInputStream dataInputStream = new DataInputStream(fileInputStream);

			UUID prevUuid = ownerUuid;
			deserialize(dataInputStream);

			if (prevUuid != ownerUuid)
				throw new IOException(
						"Mismatched UUIDs: (" + prevUuid + " => " + ownerUuid + "). File might be corrupt");

			togglePacketHandler(false);
			return;
		}

		this.ownerUuid = player.getUniqueId();
		this.world = PacketPlots.getPlotConfig().getWorld();

		HashSet<ChunkCoordIntPair> coordIntPairs = PacketPlots.getPlotConfig().getVirtualChunks();
		this.virtualChunks = new VirtualChunk[coordIntPairs.size()];

		int i = 0;
		for (ChunkCoordIntPair coordIntPair : coordIntPairs) {
			virtualChunks[i++] = new VirtualChunk(coordIntPair);
		}

		FileOutputStream fileOutputStream = new FileOutputStream(dataFile);
		DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
		serialize(dataOutputStream);

		togglePacketHandler(false);
	}

	public VirtualEnvironment(DataInputStream stream) throws IOException {
		deserialize(stream);

		Player owner = getOwner();
		if (owner == null || !owner.isOnline())
			throw new IOException("Tried initialization of VirtualEnvironment for offline player: " + ownerUuid);
		virtualEnvironments.put(owner, this);

		togglePacketHandler(false);
	}

	/**
	 * Simply stops the packet interceptor - doesn't send any new packets to
	 * override the virtualized chunk for the player.
	 */
	public void stopVirtualization() {
		togglePacketHandler(true);
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
	public void deserialize(DataInputStream stream) throws IOException {
		this.ownerUuid = new UUID(stream.readLong(), stream.readLong());
		this.world = Bukkit.getWorld(new UUID(stream.readLong(), stream.readLong()));

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
	public void serialize(DataOutputStream stream) throws IOException {
		stream.writeLong(ownerUuid.getMostSignificantBits());
		stream.writeLong(ownerUuid.getLeastSignificantBits());

		stream.writeLong(world.getUID().getMostSignificantBits());
		stream.writeLong(world.getUID().getLeastSignificantBits());

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
			// Packets to cancel/modify if they occur in the environment

			// Chunks:
			// Map Chunk Bulk
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Map_Chunk_Bulk)
			// Chunk Data (https://minecraft.wiki/w/Protocol?oldid=2772100#Chunk_Data)

			// Block updates:
			// Block change (https://minecraft.wiki/w/Protocol?oldid=2772100#Block_Change)
			// Multi block change
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Multi_Block_Change)
			// Block action (https://minecraft.wiki/w/Protocol?oldid=2772100#Block_Action)

			super.write(chx, packet, promise);
		}

		// Incomming
		@Override
		public void channelRead(ChannelHandlerContext chx, Object packet) throws Exception {
			// Packets to pass to the env. if they occur there

			// Interactions
			// Player Digging
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Digging)
			// Player Block Placement
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Block_Placement)
			// Update Sign (https://minecraft.wiki/w/Protocol?oldid=2772100#Update_Sign)

			super.channelRead(chx, packet);
		}

	}

	static {
		DATA_DIRECTORY.mkdirs();
	}

}
