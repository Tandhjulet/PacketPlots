package net.dashmc.plots.plot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Player;

import net.dashmc.plots.data.IDataHolder;
import net.dashmc.plots.pipeline.RenderPipeline;
import net.dashmc.plots.plot.CuboidEnvironment.InteractManager;
import net.dashmc.plots.plot.data.BlockBag;
import net.dashmc.plots.plot.data.VirtualChunk;
import net.dashmc.plots.utils.CuboidRegion;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockAccess;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.TileEntity;

public interface IEnvironment extends IDataHolder, IBlockAccess {

	HashSet<VirtualConnection> getConnections();

	HashMap<Integer, VirtualChunk> getVirtualChunks();

	List<TileEntity> getTileEntities();

	World getWorld();

	net.minecraft.server.v1_8_R3.World getNmsWorld();

	UUID getOwnerUuid();

	InteractManager getInteractManager();

	CuboidRegion getRegion();

	RenderPipeline getRenderPipeline();

	void setRenderPipeline(RenderPipeline renderPipeline);

	/**
	 * Not yet implemented - entities arent supported
	 * 
	 * @param bb
	 * @param entity
	 * @return
	 */
	boolean isNoOtherEntitiesInside(AxisAlignedBB bb, Entity entity);

	// https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/World.java#L2664
	boolean isBuildable(Block block, BlockPosition pos, boolean ignoreCollision, EnumDirection dir,
			Entity entity,
			ItemStack itemStack);

	boolean canPlace(BlockPosition pos, EnumDirection dir, ItemStack itemStack, EntityHuman player);

	void broadcastPacket(Packet<?> packet);

	void broadcastTile(TileEntity tile);

	void sendTile(TileEntity tile, EntityPlayer to);

	void sendTiles(EntityPlayer to);

	void sendTiles(Player to);

	void close();

	void save();

	void stopVirtualization(EntityPlayer player);

	void startVirtualization(Player player);

	void stopVirtualization(Player player);

	void startVirtualization(EntityPlayer player);

	boolean setBlock(BlockPosition pos, IBlockData blockData, int i);

	boolean isValidLocation(BlockPosition pos);

	boolean isValidLocation(int x, int y, int z);

	Player getOwner();

	EntityPlayer getNMSOwner();

	void setTileEntity(BlockPosition blockPosition, TileEntity tileEntity);

	BlockBag getBlockBag();

}