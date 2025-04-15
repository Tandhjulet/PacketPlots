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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.NumberConversions;
import org.bukkit.event.Event;

import com.google.common.collect.Lists;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.config.PlotConfig.ChunkConfig;
import net.dashmc.plots.data.IDataHolder;
import net.dashmc.plots.packets.PacketModifier;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockCommand;
import net.minecraft.server.v1_8_R3.BlockDoor;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChatMessage;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumChatFormat;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.Material;
import net.minecraft.server.v1_8_R3.MathHelper;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.MovingObjectPosition;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListenerPlayIn;
import net.minecraft.server.v1_8_R3.PacketListenerPlayOut;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockPlace;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutSetSlot;
import net.minecraft.server.v1_8_R3.Slot;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.Vec3D;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings;
import net.minecraft.server.v1_8_R3.Chunk.EnumTileEntityState;

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
	private @Getter net.minecraft.server.v1_8_R3.World nmsWorld;
	private @Getter UUID ownerUuid;
	private @Getter InteractManager interactManager = new InteractManager();
	private @Getter VirtualConnection virtualConnection;

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

		virtualConnection = new VirtualConnection(MinecraftServer.getServer(), ((CraftPlayer) player).getHandle());
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

	public TileEntity getTileEntity(BlockPosition pos) {
		if (!isValidLocation(pos))
			return null;

		TileEntity tileEntity = virtualChunks.get(Utils.getCoordHash(pos)).getTileEntity(pos,
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

		VirtualChunk chunk = virtualChunks.get(Utils.getCoordHash(pos));
		chunk.setBlock(pos, blockData);
		return true;
	}

	@SuppressWarnings("deprecation")
	public boolean a(Block block, BlockPosition blockposition, boolean flag, EnumDirection enumdirection, Entity entity,
			ItemStack itemstack) {
		Block block1 = this.getType(blockposition).getBlock();

		// TODO: this has been editied - add support for entities

		// CraftBukkit start - store default return
		boolean defaultReturn = (block1.getMaterial() == Material.ORIENTABLE && block == Blocks.ANVIL ? true
				: block1.getMaterial().isReplaceable()
						&& VirtualBlock.mayPlace(block, this, blockposition, enumdirection, itemstack));
		BlockCanBuildEvent event = new BlockCanBuildEvent(
				this.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()),
				CraftMagicNumbers.getId(block), defaultReturn);
		Bukkit.getServer().getPluginManager().callEvent(event);

		return event.isBuildable();
		// CraftBukkit end
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

	public class InteractManager {
		public boolean interactResult = false;
		public boolean firedInteract = false;

		public boolean interact(EntityHuman entityHuman, ItemStack itemstack,
				BlockPosition blockposition, EnumDirection enumdirection, float f, float f1, float f2) {
			IBlockData blockdata = getType(blockposition);
			boolean result = false;
			if (blockdata.getBlock() != Blocks.AIR) {
				boolean cancelledBlock = false;

				if (!entityHuman.getBukkitEntity().isOp() && itemstack != null
						&& Block.asBlock(itemstack.getItem()) instanceof BlockCommand) {
					cancelledBlock = true;
				}

				PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(entityHuman,
						Action.RIGHT_CLICK_BLOCK, blockposition, enumdirection, itemstack, cancelledBlock);
				firedInteract = true;
				interactResult = event.useItemInHand() == Event.Result.DENY;

				if (event.useInteractedBlock() == Event.Result.DENY) {
					// If we denied a door from opening, we need to send a correcting update to the
					// client, as it already opened the door.
					if (blockdata.getBlock() instanceof BlockDoor) {
						boolean bottom = blockdata.get(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER;
						((EntityPlayer) entityHuman).playerConnection.sendPacket(new PacketPlayOutBlockChange(nmsWorld,
								bottom ? blockposition.up() : blockposition.down()));
					}
					result = (event.useItemInHand() != Event.Result.ALLOW);
				} else if (!entityHuman.isSneaking() || itemstack == null) {
					VirtualBlock.interact(VirtualEnvironment.this, blockposition, blockdata, entityHuman,
							enumdirection, f, f1,
							f2);
				}

				if (itemstack != null && !result) {
					int j1 = itemstack.getData();
					int k1 = itemstack.count;

					VirtualBlock.onPlace(null, blockposition, blockdata);
					// result = itemstack.placeItem(entityHuman, world, blockposition,
					// enumdirection, f, f1, f2);

					result = handlePlaceItem(itemstack, blockposition, enumdirection, f, f1, f2);
					Bukkit.getLogger().info("place item: " + itemstack.toString());

					// The item count should not decrement in Creative mode.
					if (getNMSOwner().playerInteractManager.isCreative()) {
						itemstack.setData(j1);
						itemstack.count = k1;
					}
				}
			}
			return result;
		}

		private boolean handlePlaceItem(ItemStack toPlace, BlockPosition pos, EnumDirection direction, float f,
				float f1, float f2) {
			// TODO: finish this (mapped after ItemStack#placeItem)
			int data = toPlace.getData();
			int count = toPlace.count;
			// toPlace.getItem().interactWith(toPlace, getNMSOwner(), nmsWorld, pos,
			// direction, count, data, count)
			boolean flag = VirtualItem.interactWith(toPlace, getNMSOwner(), VirtualEnvironment.this, pos, direction, f,
					f1, f2);
			int newData = toPlace.getData();
			int newCount = toPlace.count;
			toPlace.count = count;
			toPlace.setData(data);

			return flag;
		}
	}

	// https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/PlayerConnection.java
	public class VirtualConnection {
		private final MinecraftServer minecraftServer;
		private EntityPlayer player;

		public VirtualConnection(MinecraftServer minecraftserver,
				EntityPlayer entityplayer) {
			this.minecraftServer = minecraftserver;
			this.player = entityplayer;
			// entityplayer.playerConnection = this;
		}

		public CraftPlayer getPlayer() {
			return (this.player == null) ? null : (CraftPlayer) this.player.getBukkitEntity();
		}

		private long lastPlace = -1;
		private int packets = 0;

		public void onBlockPlace(PacketPlayInBlockPlace packetplayinblockplace) {
			WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
			boolean throttled = false;
			if (lastPlace != -1 && packetplayinblockplace.timestamp - lastPlace < 30 && packets++ >= 4) {
				throttled = true;
			} else if (packetplayinblockplace.timestamp - lastPlace >= 30 || lastPlace == -1) {
				lastPlace = packetplayinblockplace.timestamp;
				packets = 0;
			}
			// Spigot end

			// CraftBukkit start
			if (this.player.dead)
				return;

			// CraftBukkit - if rightclick decremented the item, always send the update
			// packet. */
			// this is not here for CraftBukkit's own functionality; rather it is to fix
			// a notch bug where the item doesn't update correctly.
			boolean always = false;
			// CraftBukkit end

			ItemStack itemstack = this.player.inventory.getItemInHand();
			boolean flag = false;
			BlockPosition blockposition = packetplayinblockplace.a();
			EnumDirection enumdirection = EnumDirection.fromType1(packetplayinblockplace.getFace());

			this.player.resetIdleTimer();
			if (packetplayinblockplace.getFace() == 255) {
				if (itemstack == null) {
					return;
				}

				// CraftBukkit start
				int itemstackAmount = itemstack.count;
				// Spigot start - skip the event if throttled
				if (!throttled) {
					// Raytrace to look for 'rogue armswings'
					float f1 = this.player.pitch;
					float f2 = this.player.yaw;
					double d0 = this.player.locX;
					double d1 = this.player.locY + (double) this.player.getHeadHeight();
					double d2 = this.player.locZ;
					Vec3D vec3d = new Vec3D(d0, d1, d2);

					float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
					float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
					float f5 = -MathHelper.cos(-f1 * 0.017453292F);
					float f6 = MathHelper.sin(-f1 * 0.017453292F);
					float f7 = f4 * f5;
					float f8 = f3 * f5;
					double d3 = player.playerInteractManager.getGameMode() == WorldSettings.EnumGamemode.CREATIVE ? 5.0D
							: 4.5D;
					Vec3D vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
					MovingObjectPosition movingobjectposition = this.player.world.rayTrace(vec3d, vec3d1, false);

					boolean cancelled = false;
					if (movingobjectposition == null
							|| movingobjectposition.type != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
						org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory
								.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemstack);
						cancelled = event.useItemInHand() == Event.Result.DENY;
					} else {
						if (player.playerInteractManager.firedInteract) {
							player.playerInteractManager.firedInteract = false;
							cancelled = player.playerInteractManager.interactResult;
						} else {
							org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory
									.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, movingobjectposition.a(),
											movingobjectposition.direction, itemstack, true);
							cancelled = event.useItemInHand() == Event.Result.DENY;
						}
					}

					if (!cancelled) {
						this.player.playerInteractManager.useItem(this.player, this.player.world, itemstack);
					}
				}
				// Spigot end

				// CraftBukkit - notch decrements the counter by 1 in the above method with
				// food, snowballs and so forth, but he does it in a place that doesn't cause
				// the inventory update packet to get sent
				always = (itemstack.count != itemstackAmount)
						|| itemstack.getItem() == Item.getItemOf(Blocks.WATERLILY);
				// CraftBukkit end
			} else if (blockposition.getY() >= this.minecraftServer.getMaxBuildHeight() - 1
					&& (enumdirection == EnumDirection.UP
							|| blockposition.getY() >= this.minecraftServer.getMaxBuildHeight())) {
				ChatMessage chatmessage = new ChatMessage("build.tooHigh",
						new Object[] { Integer.valueOf(this.minecraftServer.getMaxBuildHeight()) });

				chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
				this.player.playerConnection.sendPacket(new PacketPlayOutChat(chatmessage));
				flag = true;
			} else {
				// CraftBukkit start - Check if we can actually do something over this large a
				// distance
				Location eyeLoc = this.getPlayer().getEyeLocation();
				double reachDistance = NumberConversions.square(eyeLoc.getX() - blockposition.getX())
						+ NumberConversions.square(eyeLoc.getY() - blockposition.getY())
						+ NumberConversions.square(eyeLoc.getZ() - blockposition.getZ());
				if (reachDistance > (this.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE ? 7 * 7 : 6 * 6)) {
					return;
				}

				if (!worldserver.getWorldBorder().a(blockposition)) {
					return;
				}

				if (this.player.e((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D,
						(double) blockposition.getZ() + 0.5D) < 64.0D
						&& !this.minecraftServer.a(worldserver, blockposition, this.player)
						&& worldserver.getWorldBorder().a(blockposition)) {
					always = throttled || !getInteractManager().interact(this.player,
							itemstack, blockposition, enumdirection, packetplayinblockplace.d(),
							packetplayinblockplace.e(), packetplayinblockplace.f());
				}

				flag = true;
			}

			if (flag) {
				this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
				this.player.playerConnection
						.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition.shift(enumdirection)));
			}

			itemstack = this.player.inventory.getItemInHand();
			if (itemstack != null && itemstack.count == 0) {
				this.player.inventory.items[this.player.inventory.itemInHandIndex] = null;
				itemstack = null;
			}

			if (itemstack == null || itemstack.l() == 0) {
				this.player.g = true;
				this.player.inventory.items[this.player.inventory.itemInHandIndex] = ItemStack
						.b(this.player.inventory.items[this.player.inventory.itemInHandIndex]);
				Slot slot = this.player.activeContainer.getSlot(this.player.inventory,
						this.player.inventory.itemInHandIndex);

				this.player.activeContainer.b();
				this.player.g = false;
				// CraftBukkit - TODO CHECK IF NEEDED -- new if structure might not need
				// 'always'. Kept it in for now, but may be able to remove in future
				if (!ItemStack.matches(this.player.inventory.getItemInHand(), packetplayinblockplace.getItemStack())
						|| always) {
					player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId,
							slot.rawSlotIndex, this.player.inventory.getItemInHand()));
				}
			}

		}
	}

	static {
		DATA_DIRECTORY.mkdirs();
	}

}
