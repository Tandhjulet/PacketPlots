package net.dashmc.plots.plot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
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

import lombok.Getter;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.config.PlotConfig.ChunkConfig;
import net.dashmc.plots.data.IDataHolder;
import net.dashmc.plots.events.VirtualBlockBreakEvent;
import net.dashmc.plots.events.VirtualBlockCanBuildEvent;
import net.dashmc.plots.events.VirtualBlockDamageEvent;
import net.dashmc.plots.events.VirtualInteractEvent;
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
import net.minecraft.server.v1_8_R3.WorldSettings.EnumGamemode;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IInventory;
import net.minecraft.server.v1_8_R3.ITileInventory;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.ItemSword;
import net.minecraft.server.v1_8_R3.Material;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
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
	private static final File DATA_DIRECTORY = new File(PacketPlots.getInstance().getDataFolder(), "data");

	// private @Getter HashMap<Integer, Entity> entities = new HashMap<>();
	private @Getter HashMap<Integer, VirtualChunk> virtualChunks = new HashMap<>();
	private @Getter final List<TileEntity> tileEntities = Lists.newArrayList();
	private @Getter World world;
	private @Getter net.minecraft.server.v1_8_R3.World nmsWorld;
	private @Getter UUID ownerUuid;
	private @Getter InteractManager interactManager = new InteractManager();

	public static Collection<VirtualEnvironment> getActive() {
		return virtualEnvironments.values();
	}

	public static VirtualEnvironment get(Player player) {
		return virtualEnvironments.get(player);
	}

	public VirtualEnvironment(Player player) throws IOException {
		if (player == null || !player.isOnline())
			throw new IOException(
					"Tried initialization of VirtualEnvironment for offline player: " + player.getUniqueId());

		EntityPlayer nmsOwner = ((CraftPlayer) player).getHandle();
		File dataFile = new File(DATA_DIRECTORY, player.getUniqueId() + ".dat");

		if (dataFile.exists()) {
			FileInputStream fileInputStream = new FileInputStream(dataFile);
			DataInputStream dataInputStream = new DataInputStream(fileInputStream);

			UUID prevUuid = player.getUniqueId();
			deserialize(dataInputStream);

			if (!prevUuid.equals(ownerUuid))
				throw new IOException(
						"Mismatched UUIDs: (" + prevUuid + " => " + ownerUuid + "). File might be corrupt");

			virtualEnvironments.put(player, this);
			startVirtualization(nmsOwner);
			return;
		}

		virtualEnvironments.put(player, this);

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
		startVirtualization(nmsOwner);
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

	public void close() {
		save();

		// TODO: handle active connections
		virtualEnvironments.remove(getOwner());
		virtualChunks.clear();
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
		EntityPlayer nmsOwner = ((CraftPlayer) owner).getHandle();
		if (owner == null || !owner.isOnline())
			throw new IOException("Tried initialization of VirtualEnvironment for offline player: " + ownerUuid);
		virtualEnvironments.put(owner, this);

		startVirtualization(nmsOwner);
	}

	public void stopVirtualization(EntityPlayer player) {
		getVirtualChunks().values().forEach((val) -> {
			Packet<?> packet = new PacketPlayOutMapChunk(val.getChunk(), false, 65535);
			player.playerConnection.sendPacket(packet);
		});

		VirtualConnection.get(player).close();
	}

	public void startVirtualization(EntityPlayer player) {
		VirtualConnection.establish(player, this);

		getVirtualChunks().values().forEach((val) -> {
			player.playerConnection
					.sendPacket(val.getPacket(65535, !((CraftWorld) world).getHandle().worldProvider.o(), false));

		});
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

	// public boolean addEntity(Entity entity) {
	// if (entity == null)
	// return false;
	// int x = MathHelper.floor(entity.locX / 16D);
	// int z = MathHelper.floor(entity.locZ / 16D);

	// Cancellable event = null;
	// if ((entity instanceof EntityLiving || entity.getBukkitEntity() instanceof
	// Projectile
	// || entity instanceof EntityExperienceOrb) && !(entity instanceof
	// EntityPlayer)) {
	// entity.dead = true;
	// return false;
	// } else if (entity instanceof EntityItem) {
	// Item ent = (Item) entity.getBukkitEntity();

	// VirtualItemSpawnEvent ev = new VirtualItemSpawnEvent(ent.getLocation(), ent,
	// this);
	// Bukkit.getPluginManager().callEvent(ev);
	// event = ev;
	// }

	// if (event != null && (event.isCancelled() || entity.dead)) {
	// entity.dead = true;
	// return false;
	// }

	// entities.put(entity.getId(), entity);
	// return true;
	// }

	// public void addEntity(BlockPosition blockposition, ItemStack itemstack) {
	// float f = 0.5F;
	// double d0 = (double)(nmsWorld.random.nextFloat() * f) + (double)(1.0F - f) *
	// 0.5;
	// double d1 = (double)(nmsWorld.random.nextFloat() * f) + (double)(1.0F - f) *
	// 0.5;
	// double d2 = (double)(nmsWorld.random.nextFloat() * f) + (double)(1.0F - f) *
	// 0.5;
	// EntityItem entityitem = new EntityItem(nmsWorld, (double)blockposition.getX()
	// + d0, (double)blockposition.getY() + d1, (double)blockposition.getZ() + d2,
	// itemstack);
	// entityitem.p();
	// world.addEntity(entityitem);
	// }

	public Player getOwner() {
		return Bukkit.getPlayer(ownerUuid);
	}

	public EntityPlayer getNMSOwner() {
		return ((CraftPlayer) getOwner()).getHandle();
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

		Debug.log("Loaded chunks from save file: " + virtualChunks.size());
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

		// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerInteractManager.java#L105
		public void startDestroy(EntityPlayer player, BlockPosition pos, EnumDirection dir) {
			VirtualInteractEvent event = new VirtualInteractEvent(player, Action.LEFT_CLICK_BLOCK, pos, dir,
					player.inventory.getItemInHand(), false,
					VirtualEnvironment.this);
			Bukkit.getPluginManager().callEvent(event);

			Debug.log("startDestroy called, is interact event cancelled? " + event.isCancelled());

			if (event.isCancelled()) {
				player.playerConnection
						.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());
				TileEntity tile = getTileEntity(pos);
				if (tile != null)
					player.playerConnection.sendPacket(tile.getUpdatePacket());
				return;
			}

			Debug.log("Is player creative?" + player.playerInteractManager.isCreative());

			if (player.playerInteractManager.isCreative()) {
				breakBlock(player, pos);
				return;
			}

			Block block = getType(pos).getBlock();

			Debug.log("Is survival or adventure mode?" + player.playerInteractManager.c());

			if (player.playerInteractManager.c()) {
				if (player.playerInteractManager.getGameMode() == EnumGamemode.SPECTATOR)
					return;

				if (player.cn()) {
					ItemStack item = player.bZ();
					if (item == null)
						return;
					if (item.c(block))
						return;
				}
			}

			Utils.setLastDigTick(player.playerInteractManager, Utils.getCurrentTick(player.playerInteractManager));
			float f = 1f;

			if (event.getUseClickedBlock() == Event.Result.DENY) {

				IBlockData data = getType(pos);
				if (block == Blocks.WOODEN_DOOR) {
					boolean bottom = data.get(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER;
					player.playerConnection
							.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());
					player.playerConnection.sendPacket(
							new VirtualBlockChangePacket(VirtualEnvironment.this, bottom ? pos.up() : pos.down())
									.getPacket());
				} else if (block == Blocks.TRAPDOOR) {
					player.playerConnection
							.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());
				}
			} else if (block.getMaterial() != Material.AIR) {
				// block.attack(nmsWorld, pos, player);
				f = block.getDamage(player, nmsWorld, pos);
			}

			if (event.getUseItemInHand() == Event.Result.DENY) {
				if (f > 1.0f)
					player.playerConnection
							.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());

				return;
			}

			VirtualBlockDamageEvent blockEvent = new VirtualBlockDamageEvent(Utils.convertPosToLoc(world, pos),
					player.getBukkitEntity(), player.getBukkitEntity().getItemInHand(), f >= 1.0f,
					VirtualEnvironment.this);
			if (blockEvent.isCancelled()) {
				player.playerConnection
						.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());
				return;
			}

			if (blockEvent.isInstaBreak())
				f = 2.0f;

			if (block.getMaterial() != Material.AIR && f >= 1.0f) {
				breakBlock(player, pos);
			} else { // https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerInteractManager.java#L190
				Utils.setIsDestroying(player.playerInteractManager, true);
				Utils.setDestroyPosition(player.playerInteractManager, pos);
				Utils.setForce(player.playerInteractManager, (int) (f * 10f));
			}

			return;
		}

		// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerInteractManager.java#L203
		public void stopDestroy(EntityPlayer player, BlockPosition pos) {
			if (pos.equals(Utils.getDestroyPosition(player.playerInteractManager))) {
				Utils.setCurrentTick(player.playerInteractManager, MinecraftServer.currentTick);
				int diggingFor = Utils.getCurrentTick(player.playerInteractManager)
						- Utils.getLastDigTick(player.playerInteractManager);
				Block block = getType(pos).getBlock();
				if (block.getMaterial() != Material.AIR) {
					float f = block.getDamage(player, nmsWorld, pos) * (float) (diggingFor + 1);
					if (f >= 0.7F) {
						Utils.setIsDestroying(player.playerInteractManager, false);
						breakBlock(player, pos);
					} else { // TODO:

					}
				}
			} else {
				player.playerConnection
						.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());
			}
		}

		// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerInteractManager.java#L231
		public void abortDestory(EntityPlayer player) {
			Utils.setIsDestroying(player.playerInteractManager, false);
			return;
		}

		// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerInteractManager.java#L249
		public boolean breakBlock(EntityPlayer player, BlockPosition pos) {
			VirtualBlockBreakEvent ev = null;

			boolean isSword = player.playerInteractManager.getGameMode().d() && player.bA() != null
					&& player.bA().getItem() instanceof ItemSword;
			if (getTileEntity(pos) == null && !isSword) {
				PacketPlayOutBlockChange packet = new VirtualBlockChangePacket(VirtualEnvironment.this, pos)
						.getPacket();
				packet.block = Blocks.AIR.getBlockData();
				player.playerConnection.sendPacket(packet);

				Debug.log("is not tile && is not sword, player is creative. sending break packet.");
			}

			ev = new VirtualBlockBreakEvent(Utils.convertPosToLoc(world, pos), VirtualEnvironment.this);
			ev.setCancelled(isSword);

			IBlockData nmsData = getType(pos);
			Block nmsBlock = nmsData.getBlock();

			// if(nmsBlock != null && !ev.isCancelled() &&
			// !player.playerInteractManager.isCreative() && player.b(nmsBlock)) {
			// if(!(nmsBlock.d() && !nmsBlock.isTileEntity() &&
			// EnchantmentManager.hasSilkTouchEnchantment(player))) {

			// }
			// }

			Bukkit.getPluginManager().callEvent(ev);
			TileEntity tile = getTileEntity(pos);

			Debug.log("Is block break event cancelled? " + ev.isCancelled());

			if (ev.isCancelled()) {
				if (isSword)
					return false;

				player.playerConnection
						.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());

				if (tile != null)
					player.playerConnection.sendPacket(tile.getUpdatePacket());

				return false;
			}

			if (nmsBlock == Blocks.AIR)
				return false;

			Debug.log("block is not air");

			if (nmsBlock == Blocks.SKULL && !player.playerInteractManager.isCreative()) {
				boolean flag = setBlock(pos, Blocks.AIR.getBlockData(), 3);
				// postBreak excluded:
				// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerInteractManager.java#L243

				return flag;
			}

			if (player.playerInteractManager.c()) {
				if (player.playerInteractManager.getGameMode() == EnumGamemode.SPECTATOR)
					return false;
				if (!player.cn()) {
					ItemStack item = player.bZ();
					if (item == null)
						return false;
					if (!item.c(nmsBlock))
						return false;
				}
			}

			boolean couldSet = setBlock(pos, Blocks.AIR.getBlockData(), 3);
			Debug.log("tried setting block server-side at " + pos.toString() + " to air");

			if (player.playerInteractManager.isCreative())
				player.playerConnection
						.sendPacket(new VirtualBlockChangePacket(VirtualEnvironment.this, pos).getPacket());
			else {
				ItemStack held = player.bZ();
				boolean flag = player.b(nmsBlock);

				if (held != null) {
					held.a(null, nmsBlock, pos, player);
					if (held.count == 0)
						player.ca();
				}

				if (flag && couldSet) {
					// here the item would drop normally - we won't do that :-)
					// nmsBlock.a(nmsWorld, player, pos, nmsData, tile);
				}
			}

			return couldSet;
		}
	}

	static {
		DATA_DIRECTORY.mkdirs();
	}

}
