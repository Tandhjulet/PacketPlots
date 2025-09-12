package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockStone;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockStone extends VirtualBlock<BlockStone> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockStone.EnumStoneVariant) iblockdata.get(BlockStone.VARIANT)).a();
	}

	@Override
	public Class<BlockStone> getClazz() {
		return BlockStone.class;
	}

}
