package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockWood;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockWood extends VirtualBlock<BlockWood> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockWood.EnumLogVariant) iblockdata.get(BlockWood.VARIANT)).a();
	}

	@Override
	public Class<BlockWood> getClazz() {
		return BlockWood.class;
	}

}
