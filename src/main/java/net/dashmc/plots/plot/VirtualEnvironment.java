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
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
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
import net.minecraft.server.v1_8_R3.PacketListenerPlayOut;

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
	private static final HashMap<Class<?>, PacketModifier<?>> packetModifiers = new HashMap<>();

	private static final String NETTY_PIPELINE_NAME = "VirtualEnvironment";
	private static final File DATA_DIRECTORY = new File(PacketPlots.getInstance().getDataFolder(), "data");

	private @Getter HashMap<Integer, VirtualChunk> virtualChunks = new HashMap<>();
	private @Getter World world;
	private @Getter UUID ownerUuid;

	public static <T extends Packet<?>> void register(PacketModifier<T> modifier) {
		packetModifiers.put(modifier.getClazz(), modifier);
	}

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

			UUID prevUuid = player.getUniqueId();
			deserialize(dataInputStream);

			if (!prevUuid.equals(ownerUuid))
				throw new IOException(
						"Mismatched UUIDs: (" + prevUuid + " => " + ownerUuid + "). File might be corrupt");

			togglePacketHandler(false);
			return;
		}

		this.ownerUuid = player.getUniqueId();
		this.world = PacketPlots.getPlotConfig().getWorld();
		net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) world).getHandle();

		HashSet<ChunkCoordIntPair> coordIntPairs = PacketPlots.getPlotConfig().getVirtualChunks();

		for (ChunkCoordIntPair coordIntPair : coordIntPairs) {
			virtualChunks.put(coordIntPair.hashCode(), new VirtualChunk(coordIntPair, nmsWorld));
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

	public EntityPlayer getNMSOwner() {
		return ((CraftPlayer) getOwner()).getHandle();
	}

	@SuppressWarnings("unchecked")
	public <T extends Packet<?>> boolean intercept(T packet) {
		PacketModifier<T> modifier = (PacketModifier<T>) packetModifiers.get(packet.getClass());
		if (modifier != null) {
			return modifier.modify(packet, this);
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deserialize(DataInputStream stream) throws IOException {
		this.ownerUuid = new UUID(stream.readLong(), stream.readLong());
		this.world = Bukkit.getWorld(new UUID(stream.readLong(), stream.readLong()));
		net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) world).getHandle();

		int arraySize = stream.readInt();

		HashSet<ChunkCoordIntPair> chunkCoordPairs = (HashSet<ChunkCoordIntPair>) PacketPlots.getPlotConfig()
				.getVirtualChunks().clone();

		// Read in the saved chunks. If any of them aren't specified as virtual in the
		// config any more discard them:
		for (int i = 0; i < arraySize; i++) {
			VirtualChunk chunk = new VirtualChunk(((CraftWorld) world).getHandle(), stream);
			if (!chunkCoordPairs.contains(chunk.getCoordPair()))
				continue;

			chunkCoordPairs.remove(chunk.getCoordPair());
			virtualChunks.put(chunk.getCoordPair().hashCode(), chunk);
		}

		// Fill the remaining spots, if any, with new virtual chunks
		for (ChunkCoordIntPair pair : chunkCoordPairs) {
			virtualChunks.put(pair.hashCode(), new VirtualChunk(pair, nmsWorld));
		}

	}

	@Override
	public void serialize(DataOutputStream stream) throws IOException {
		stream.writeLong(ownerUuid.getMostSignificantBits());
		stream.writeLong(ownerUuid.getLeastSignificantBits());

		stream.writeLong(world.getUID().getMostSignificantBits());
		stream.writeLong(world.getUID().getLeastSignificantBits());

		stream.writeInt(virtualChunks.size());

		for (VirtualChunk virtualChunk : virtualChunks.values()) {
			virtualChunk.serialize(stream);
		}

	}

	/**
	 * PacketHandler for this virtual environment. Sorts through the packets and
	 * passes the important ones to VirtualEnvironment#intercept.
	 */
	private class PacketHandler extends ChannelDuplexHandler {
		// Outgoing
		@SuppressWarnings("unchecked")
		@Override
		public void write(ChannelHandlerContext chx, Object obj, ChannelPromise promise) throws Exception {
			// Packets to cancel/modify if they occur in the environment

			// Block updates:
			// Block change (https://minecraft.wiki/w/Protocol?oldid=2772100#Block_Change)
			// Multi block change
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Multi_Block_Change)
			// Block action (https://minecraft.wiki/w/Protocol?oldid=2772100#Block_Action)

			Packet<PacketListenerPlayOut> packet = (Packet<PacketListenerPlayOut>) obj;
			if (intercept(packet))
				return;

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
