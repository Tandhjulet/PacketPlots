package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockCloth;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockWool extends VirtualBlock<BlockCloth> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((EnumColor) iblockdata.get(BlockCloth.COLOR)).getColorIndex();
	}

	@Override
	public Class<BlockCloth> getClazz() {
		return BlockCloth.class;
	}

}
