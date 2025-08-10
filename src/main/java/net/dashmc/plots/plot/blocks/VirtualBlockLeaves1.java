package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.BlockLeaves1;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockWood;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockLeaves1 extends VirtualBlock<BlockLeaves1> {

	@Override
	public boolean interact(BlockLeaves1 block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return false;
	}

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockWood.EnumLogVariant) iblockdata.get(BlockLeaves1.VARIANT)).a();
	}

	@Override
	public Class<BlockLeaves1> getClazz() {
		return BlockLeaves1.class;
	}

}
