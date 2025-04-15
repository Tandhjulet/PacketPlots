package net.dashmc.plots.plot.items;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.plot.VirtualItem;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.ItemBlock;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.TileEntity;

// https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/ItemBlock.java#L16
public class VirtualBlockItem extends VirtualItem<ItemBlock> {

	@Override
	public boolean interactWith(ItemBlock block, ItemStack itemStack, EntityHuman entityHuman,
			VirtualEnvironment environment, BlockPosition blockposition, EnumDirection enumdirection, float f, float f1,
			float f2) {
		// TODO: add support for layer checking in the virtualblock
		// if (!currBlock.a(environment, blockposition)) {
		// blockposition = blockposition.shift(enumdirection);
		// }
		blockposition = blockposition.shift(enumdirection);

		if (itemStack.count == 0) {
			return false;
		} else if (!entityHuman.a(blockposition, enumdirection, itemStack)) {
			return false;
		} else if (environment.a(block.d(), blockposition, false, enumdirection, (Entity) null, itemStack)) {
			// TODO: add block get placed state into VirtualBlock
			IBlockData iblockdata1 = block.d().getBlockData();

			if (environment.setBlock(blockposition, iblockdata1, 3)) {
				iblockdata1 = environment.getType(blockposition);
				if (iblockdata1.getBlock() == block.d()) {
					a(environment, blockposition, itemStack);

					VirtualBlock.postPlace(block.d(), environment, blockposition, iblockdata1, entityHuman, itemStack);
					// block.d().postPlace(environment, blockposition, iblockdata1, entityHuman,
					// itemStack);
				}

				--itemStack.count;
			}

			return true;
		} else {
			return false;
		}
	}

	public static boolean a(VirtualEnvironment environment, BlockPosition blockposition, ItemStack itemstack) {
		if (itemstack.hasTag() && itemstack.getTag().hasKeyOfType("BlockEntityTag", 10)) {
			TileEntity tileentity = environment.getTileEntity(blockposition);

			if (tileentity != null) {
				NBTTagCompound nbttagcompound = new NBTTagCompound();
				NBTTagCompound nbttagcompound1 = (NBTTagCompound) nbttagcompound.clone();

				tileentity.b(nbttagcompound);
				NBTTagCompound nbttagcompound2 = (NBTTagCompound) itemstack.getTag().get("BlockEntityTag");

				nbttagcompound.a(nbttagcompound2);
				nbttagcompound.setInt("x", blockposition.getX());
				nbttagcompound.setInt("y", blockposition.getY());
				nbttagcompound.setInt("z", blockposition.getZ());
				if (!nbttagcompound.equals(nbttagcompound1)) {
					tileentity.a(nbttagcompound);
					tileentity.update();
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public Class<ItemBlock> getClazz() {
		return ItemBlock.class;
	}

}
