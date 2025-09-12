package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockLog2;
import net.minecraft.server.v1_8_R3.BlockWood;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockLog2 extends VirtualBlock<BlockLog2> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockWood.EnumLogVariant) iblockdata.get(BlockLog2.VARIANT)).a() - 4;
	}

	@Override
	public Class<BlockLog2> getClazz() {
		return BlockLog2.class;
	}

}
