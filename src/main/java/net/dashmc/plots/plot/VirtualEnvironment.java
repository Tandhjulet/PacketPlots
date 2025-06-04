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
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;

import com.google.common.collect.Lists;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.config.PlotConfig.ChunkConfig;
import net.dashmc.plots.data.IDataHolder;
import net.dashmc.plots.events.VirtualBlockCanBuildEvent;
import net.dashmc.plots.events.VirtualInteractEvent;
import net.dashmc.plots.packets.PacketModifier;
import net.dashmc.plots.packets.extensions.VirtualBlockChangePacket;
import net.dashmc.plots.utils.Debug;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockCommand;
import net.minecraft.server.v1_8_R3.BlockDoor;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.Chunk.EnumTileEntityState;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IInventory;
import net.minecraft.server.v1_8_R3.ITileInventory;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.Material;
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
	private static final HashMap<Player, VirtualEnvironment> virtualEnvironments = new HashMap<>();
	private static final HashMap<Class<?>, PacketModifier<?>> packetModifiers = new HashMap<>();

	private static final String NETTY_PIPELINE_NAME = "VirtualEnvironment";
	private static final File DATA_DIRECTORY = new File(PacketPlots.getInstance().getDataFolder(), "data");

	private @Getter HashMap<Integer, VirtualChunk> virtualChunks = new HashMap<>();
	private @Getter final List<TileEntity> tileEntities = Lists.newArrayList();
	private @Getter World world;
	private @Getter net.minecraft.server.v1_8_R3.World nmsWorld;
	private @Getter UUID ownerUuid;
	private @Getter InteractManager interactManager = new InteractManager();

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
		this.nmsWorld = ((CraftWorld) world).getHandle();
		net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) world).getHandle();

		HashSet<ChunkConfig> chunks = PacketPlots.getPlotConfig().getVirtualChunks();

		for (ChunkConfig chunk : chunks) {
			virtualChunks.put(chunk.coords.hashCode(),
					new VirtualChunk(this, chunk.coords, nmsWorld, chunk.getSectionsAsMask()));
		}

		save();
		togglePacketHandler(false);
	}

	/**
	 * Not yet implemented - entities arent supported
	 * 
	 * @param bb
	 * @param entity
	 * @return
	 */
	public boolean isNoOtherEntitiesInside(AxisAlignedBB bb, Entity entity) {
		return true;
	}

	// https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/World.java#L2664
	public boolean isBuildable(Block block, BlockPosition pos, boolean hasBoundingBox, EnumDirection dir, Entity entity,
			ItemStack itemStack) {
		Block curr = getType(pos).getBlock();
		AxisAlignedBB bb = hasBoundingBox ? null
				: block.a((net.minecraft.server.v1_8_R3.World) null, pos, curr.getBlockData());

		final boolean defaultReturn;
		if (bb != null && !isNoOtherEntitiesInside(bb, entity)) {
			defaultReturn = false;
		} else if (curr.getMaterial() == Material.ORIENTABLE && block == Blocks.ANVIL) {
			defaultReturn = true;
		} else {
			Debug.log("isReplaceable: " + curr.getMaterial().isReplaceable());
			Debug.log("mayPlace: " + VirtualBlock.mayPlace(block, this, pos, dir, itemStack) + " (pos: "
					+ pos.toString() + ")");

			defaultReturn = curr.getMaterial().isReplaceable()
					&& VirtualBlock.mayPlace(block, this, pos, dir, itemStack);
		}

		@SuppressWarnings("deprecation")
		VirtualBlockCanBuildEvent event = new VirtualBlockCanBuildEvent(defaultReturn,
				Utils.convertPosToLoc(world, pos), org.bukkit.Material.getMaterial(CraftMagicNumbers.getId(curr)),
				this);
		Bukkit.getPluginManager().callEvent(event);

		return event.isBuildable();
	}

	public boolean canPlace(BlockPosition pos, EnumDirection dir, ItemStack itemStack, EntityHuman player) {
		if (player.abilities.mayBuild)
			return true;
		if (itemStack == null)
			return false;

		BlockPosition shifted = pos.shift(dir.opposite());
		Block block = getType(shifted).getBlock();
		return itemStack.d(block) || itemStack.x();
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

		VirtualChunk chunk = virtualChunks.get(Utils.getChunkCoordHash(pos));
		return chunk.getBlockData(pos);
	}

	public TileEntity getTileEntity(BlockPosition pos) {
		if (!isValidLocation(pos))
			return null;

		TileEntity tileEntity = virtualChunks.get(Utils.getChunkCoordHash(pos)).getTileEntity(pos,
				EnumTileEntityState.IMMEDIATE);
		if (tileEntity == null) {
			for (int i = 0; i < tileEntities.size(); i++) {
				TileEntity tile = tileEntities.get(i);
				if (!tile.x() && tile.getPosition().equals(pos)) {
					tileEntity = tile;
					break;
				}
			}
		}

		return tileEntity;
	}

	public boolean setBlock(BlockPosition pos, IBlockData blockData, int i) {
		if (!this.isValidLocation(pos))
			return false;

		VirtualChunk chunk = virtualChunks.get(Utils.getChunkCoordHash(pos));
		boolean succeeded = chunk.setBlock(pos, blockData);
		return succeeded;
	}

	public boolean isValidLocation(BlockPosition blockposition) {
		if (!(blockposition.getX() >= -30000000 && blockposition.getZ() >= -30000000 && blockposition.getX() < 30000000
				&& blockposition.getZ() < 30000000 && blockposition.getY() >= 0 && blockposition.getY() < 256))
			return false;

		int hash = Utils.getChunkCoordHash(blockposition);
		VirtualChunk chunk = virtualChunks.get(hash);
		if (chunk == null)
			return false;

		return chunk.isSectionSet((byte) (blockposition.getY() >> 4));
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
			this.getVirtualChunks().get(Utils.getChunkCoordHash(blockPosition)).setTileEntity(blockPosition,
					tileEntity);
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
			if (intercept(packet))
				return;

			super.channelRead(chx, packet);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.err.println("Netty pipeline exception: " + cause);
			cause.printStackTrace();
			super.exceptionCaught(ctx, cause);
		}
	}

	public class InteractManager {
		public boolean interact(EntityHuman human, VirtualEnvironment env, ItemStack item, BlockPosition pos,
				EnumDirection dir, float cX, float cY, float cZ) {
			IBlockData blockData = env.getType(pos);
			boolean result = false;

			Debug.log("Interact position: " + pos.toString() + " (dir " + dir.toString() + ")");
			Debug.log("Interact IBD:" + blockData.getBlock().toString());
			Debug.log("Block is not air?: " + (blockData.getBlock() != Blocks.AIR));

			if (blockData.getBlock() != Blocks.AIR) {
				boolean cancelledBlock = false;
				GameMode gameMode = getOwner().getGameMode();

				if (gameMode == GameMode.SPECTATOR) {
					TileEntity tile = env.getTileEntity(pos);
					cancelledBlock = !(tile instanceof ITileInventory || tile instanceof IInventory);
				}

				if (!human.getBukkitEntity().isOp() && item != null
						&& Block.asBlock(item.getItem()) instanceof BlockCommand) {
					cancelledBlock = true;
				}

				VirtualInteractEvent ev = new VirtualInteractEvent(human, Action.RIGHT_CLICK_BLOCK, pos, dir, item,
						cancelledBlock, env);
				if (cancelledBlock)
					ev.setUseClickedBlock(Event.Result.DENY);
				Bukkit.getPluginManager().callEvent(ev);

				if (ev.getUseClickedBlock() == Event.Result.DENY) {
					if (blockData.getBlock() instanceof BlockDoor) {
						boolean bottom = blockData.get(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER;
						((EntityPlayer) human).playerConnection
								.sendPacket(
										new VirtualBlockChangePacket(env, bottom ? pos.up() : pos.down()).getPacket());
					}
					result = ev.getUseItemInHand() != Event.Result.ALLOW;
				} else if (gameMode == GameMode.SPECTATOR) {
					// TODO: implement
				} else if (!human.isSneaking() || item == null) {
					result = VirtualBlock.interact(env, pos, blockData, human, dir, cX, cY, cZ);
				}

				Debug.log("Item is not null: " + (item != null));
				Debug.log("Result is: " + result);

				if (item != null && !result) {
					int data = item.getData();
					int count = item.count;

					Debug.log("Placing item " + item.getItem().getName() + " (" + item.getItem().getClass() + ")");

					result = VirtualItem.placeItem(item, human, env, pos, dir, cX, cY, cZ);

					if (gameMode == GameMode.CREATIVE) {
						item.setData(data);
						item.count = count;
					}
				}
			}

			return result;
		}
	}

	static {
		DATA_DIRECTORY.mkdirs();
	}

}
