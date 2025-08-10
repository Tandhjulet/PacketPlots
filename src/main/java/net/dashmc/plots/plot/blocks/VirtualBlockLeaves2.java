package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.BlockLeaves2;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockWood;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockLeaves2 extends VirtualBlock<BlockLeaves2> {

	@Override
	public boolean interact(BlockLeaves2 block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return false;
	}

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockWood.EnumLogVariant) iblockdata.get(BlockLeaves2.VARIANT)).a() - 4;
	}

	@Override
	public Class<BlockLeaves2> getClazz() {
		return BlockLeaves2.class;
	}

}
