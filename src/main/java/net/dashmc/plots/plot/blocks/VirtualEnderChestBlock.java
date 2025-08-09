package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.plot.wrappers.EnderChestTileWrapper;
import net.minecraft.server.v1_8_R3.BlockEnderChest;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.InventoryEnderChest;
import net.minecraft.server.v1_8_R3.StatisticList;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.TileEntityEnderChest;

public class VirtualEnderChestBlock extends VirtualBlock<BlockEnderChest> {

	@Override
	public boolean interact(BlockEnderChest block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		InventoryEnderChest inventory = entityhuman.getEnderChest();
		TileEntity tile = environment.getTileEntity(blockposition);

		if (inventory == null)
			return true;
		if (!(tile instanceof TileEntityEnderChest))
			return true;

		if (environment.getType(blockposition.up()).getBlock().isOccluding())
			return true;

		TileEntityEnderChest enderChest = (TileEntityEnderChest) tile;
		EnderChestTileWrapper wrappedEnderChest = new EnderChestTileWrapper(environment, enderChest);

		enderChest.g++;
		inventory.a(wrappedEnderChest);
		entityhuman.openContainer(inventory);
		entityhuman.b(StatisticList.V);
		return true;

	}

	@Override
	public Class<BlockEnderChest> getClazz() {
		return BlockEnderChest.class;
	}

}
