package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.BlockBag;
import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockSkull;
import net.minecraft.server.v1_8_R3.GameProfileSerializer;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.Items;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.TileEntitySkull;

public class VirtualSkullBlock extends VirtualBlock<BlockSkull> {

	@Override
	public void onBlockHarvested(BlockSkull block, VirtualEnvironment environment, BlockPosition pos, IBlockData data,
			BlockBag bag, TileEntity tile) {
		ItemStack skullItem = new ItemStack(Items.SKULL, 1, getDropData(environment, pos));
		TileEntitySkull skullTile = (TileEntitySkull) environment.getTileEntity(pos);
		if (skullTile.getSkullType() == 3 && skullTile.getGameProfile() != null) {
			skullItem.setTag(new NBTTagCompound());
			NBTTagCompound compound = new NBTTagCompound();
			GameProfileSerializer.serialize(compound, skullTile.getGameProfile());
			skullItem.getTag().set("SkullOwner", compound);
		}

		bag.add(skullItem);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(BlockSkull block, VirtualEnvironment env, BlockPosition pos,
			IBlockData state) {
		block.updateShape(env, pos);
		return super.getCollisionBoundingBox(block, env, pos, state);
	}

	public int getDropData(VirtualEnvironment environment, BlockPosition blockposition) {
		TileEntity tileentity = environment.getTileEntity(blockposition);

		return tileentity instanceof TileEntitySkull ? ((TileEntitySkull) tileentity).getSkullType()
				: super.getDropData(environment, blockposition);
	}

	@Override
	public Class<BlockSkull> getClazz() {
		return BlockSkull.class;
	}

}
