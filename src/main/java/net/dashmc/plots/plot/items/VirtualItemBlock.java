package net.dashmc.plots.plot.items;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.plot.VirtualItem;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.ItemBlock;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.TileEntity;

public class VirtualItemBlock extends VirtualItem<ItemBlock> {

	@Override
	public boolean interactWith(ItemStack item, EntityHuman player, VirtualEnvironment environment, BlockPosition pos,
			EnumDirection direction, float cX, float cY, float cZ) {
		IBlockData ibd = environment.getType(pos);
		Block block = ibd.getBlock();

		pos = pos.shift(direction);

		if (item.count == 0)
			return false;
		if (!environment.canPlace(pos, direction, item, player))
			return false;
		if (!environment.isBuildable(block, pos, false, direction, player, item))
			return false;

		Block toPlace = ((ItemBlock) item.getItem()).d();

		int data = item.getItem().filterData(item.getData());
		IBlockData placedBlockData = toPlace.getPlacedState(null, pos, direction, cX, cY, cZ, data, player);
		if (!environment.setBlock(pos, placedBlockData, 3))
			return false;

		IBlockData blockData = environment.getType(pos);
		if (blockData.getBlock() == toPlace) {
			initializeBlock(environment, pos, item);
			VirtualBlock.postPlace(toPlace, environment, pos, blockData, player, item);
		}
		item.count--;

		return true;
	}

	// https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/ItemBlock.java
	public static boolean initializeBlock(VirtualEnvironment env, BlockPosition pos, ItemStack itemStack) {
		if (!(itemStack.hasTag() && itemStack.getTag().hasKeyOfType("BlockEntityTag", 10)))
			return false;
		TileEntity tile = env.getTileEntity(pos);
		if (tile == null)
			return false;

		NBTTagCompound compound = new NBTTagCompound();
		NBTTagCompound nbttagcompound1 = (NBTTagCompound) compound.clone();
		tile.b(compound);

		NBTTagCompound nbttagcompound2 = (NBTTagCompound) itemStack.getTag().get("BlockEntityTag");

		compound.a(nbttagcompound2);
		compound.setInt("x", pos.getX());
		compound.setInt("y", pos.getY());
		compound.setInt("z", pos.getZ());

		if (compound.equals(nbttagcompound1))
			return false;

		tile.a(compound);
		tile.update();
		return true;
	}

	@Override
	public Class<ItemBlock> getClazz() {
		return ItemBlock.class;
	}

}
