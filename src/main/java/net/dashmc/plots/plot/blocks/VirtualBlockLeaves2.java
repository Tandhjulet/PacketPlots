package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockLeaves2;
import net.minecraft.server.v1_8_R3.BlockWood;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockLeaves2 extends VirtualBlock<BlockLeaves2> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockWood.EnumLogVariant) iblockdata.get(BlockLeaves2.VARIANT)).a() - 4;
	}

	@Override
	public Class<BlockLeaves2> getClazz() {
		return BlockLeaves2.class;
	}

}
