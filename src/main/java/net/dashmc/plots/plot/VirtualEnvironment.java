package net.dashmc.plots.plot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.config.PlotConfig.ChunkConfig;
import net.dashmc.plots.data.IDataHolder;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListenerPlayIn;
import net.minecraft.server.v1_8_R3.PacketListenerPlayOut;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.TileEntity;

/*
 * Data structure:
 * player uuid (16 bytes - 2x long)
 * world uid (16 bytes - 2x long)
 * 
 * amount of virtual chunks (4 bytes - int)
 * array of VirtualChunk
 */

public class VirtualEnvironment implements IDataHolder {
	public static boolean DEBUG = true;

	private static final HashMap<Player, VirtualEnvironment> virtualEnvironments = new HashMap<>();
	private static final HashMap<Class<?>, PacketModifier<?>> packetModifiers = new HashMap<>();

	private static final String NETTY_PIPELINE_NAME = "VirtualEnvironment";
	private static final File DATA_DIRECTORY = new File(PacketPlots.getInstance().getDataFolder(), "data");

	private @Getter HashMap<Integer, VirtualChunk> virtualChunks = new HashMap<>();
	private @Getter final List<TileEntity> tileEntities = Lists.newArrayList();
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

		HashSet<ChunkConfig> chunks = PacketPlots.getPlotConfig().getVirtualChunks();

		for (ChunkConfig chunk : chunks) {
			virtualChunks.put(chunk.coords.hashCode(),
					new VirtualChunk(this, chunk.coords, nmsWorld, chunk.getSectionsAsMask()));
		}

		save();
		togglePacketHandler(false);
	}

	public void save() {
		try {
			File dataFile = new File(DATA_DIRECTORY, getOwnerUuid() + ".dat");
			FileOutputStream fileOutputStream = new FileOutputStream(dataFile);
			DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
			serialize(dataOutputStream);

			fileOutputStream.close();
			dataOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public VirtualEnvironment(DataInputStream stream) throws IOException {
		deserialize(stream);

		Player owner = getOwner();
		if (owner == null || !owner.isOnline())
			throw new IOException("Tried initialization of VirtualEnvironment for offline player: " + ownerUuid);
		virtualEnvironments.put(owner, this);

		togglePacketHandler(false);
	}

	public void stopVirtualization() {
		togglePacketHandler(true);
		getVirtualChunks().values().forEach((val) -> {
			Packet<?> packet = new PacketPlayOutMapChunk(val.getChunk(), false, 65535);
			getNMSOwner().playerConnection.sendPacket(packet);
		});
	}

	public void startVirtualization() {
		togglePacketHandler(false);
		getVirtualChunks().values().forEach((val) -> {
			getNMSOwner().playerConnection
					.sendPacket(val.getPacket(65535, !((CraftWorld) world).getHandle().worldProvider.o(), false));

		});
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

	public IBlockData getType(BlockPosition pos) {
		if (!isValidLocation(pos))
			return Blocks.AIR.getBlockData();

		VirtualChunk chunk = virtualChunks.get(Utils.getCoordHash(pos));
		return chunk.getBlockData(pos);
	}

	public boolean setBlock(BlockPosition pos, IBlockData blockData, int i) {
		if (!this.isValidLocation(pos))
			return false;

		VirtualChunk chunk = virtualChunks.get(Utils.getCoordHash(pos));
		chunk.setBlock(pos, blockData);
		return true;
	}

	private boolean isValidLocation(BlockPosition blockposition) {
		if (!(blockposition.getX() >= -30000000 && blockposition.getZ() >= -30000000 && blockposition.getX() < 30000000
				&& blockposition.getZ() < 30000000 && blockposition.getY() >= 0 && blockposition.getY() < 256))
			return false;

		int hash = Utils.getCoordHash(blockposition);
		return virtualChunks.get(hash) != null;
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
	public void deserialize(DataInputStream stream) throws IOException {
		this.ownerUuid = new UUID(stream.readLong(), stream.readLong());
		this.world = Bukkit.getWorld(new UUID(stream.readLong(), stream.readLong()));
		net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) world).getHandle();

		int arraySize = stream.readInt();

		Map<ChunkCoordIntPair, ChunkConfig> chunkCoordPairs = PacketPlots.getPlotConfig().getVirtualChunks().stream()
				.collect(Collectors.toMap(e -> e.coords, e -> e));

		// Read in the saved chunks. If any of them aren't specified as virtual in the
		// config any more discard them:
		for (int i = 0; i < arraySize; i++) {
			VirtualChunk chunk = new VirtualChunk(this, ((CraftWorld) world).getHandle(), stream);
			if (!chunkCoordPairs.containsKey(chunk.getCoordPair()))
				continue;

			char allowedSections = chunkCoordPairs.remove(chunk.getCoordPair()).getSectionsAsMask();
			chunk.setAllowedSections(allowedSections);

			virtualChunks.put(chunk.getCoordPair().hashCode(), chunk);
		}

		// Fill the remaining spots, if any, with new virtual chunks
		for (ChunkConfig chunk : chunkCoordPairs.values()) {
			virtualChunks.put(chunk.coords.hashCode(),
					new VirtualChunk(this, chunk.coords, nmsWorld, chunk.getSectionsAsMask()));
		}
	}

	public void setTileEntity(BlockPosition blockPosition, TileEntity tileEntity) {
		if (tileEntity != null && !tileEntity.x()) {
			tileEntities.add(tileEntity);
			this.getVirtualChunks().get(Utils.getCoordHash(blockPosition)).setTileEntity(blockPosition, tileEntity);
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
		@SuppressWarnings("unchecked")
		@Override
		public void channelRead(ChannelHandlerContext chx, Object obj) throws Exception {
			// Packets to pass to the env. if they occur there

			// Interactions
			// Player Digging
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Digging)
			// Player Block Placement
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Block_Placement)
			// Update Sign (https://minecraft.wiki/w/Protocol?oldid=2772100#Update_Sign)
			Packet<PacketListenerPlayIn> packet = (Packet<PacketListenerPlayIn>) obj;

			if (DEBUG) {
				try {
					if (intercept(packet))
						return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				if (intercept(packet))
					return;
			}

			super.channelRead(chx, packet);
		}

	}

	static {
		DATA_DIRECTORY.mkdirs();
	}

}
