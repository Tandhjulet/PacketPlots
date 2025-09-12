package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockCarpet;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualCarpetBlock extends VirtualBlock<BlockCarpet> {

	@Override
	public int getDropData(IBlockData ibd) {
		return ((EnumColor) ibd.get(BlockCarpet.COLOR)).getColorIndex();
	}

	@Override
	public Class<BlockCarpet> getClazz() {
		return BlockCarpet.class;
	}

}
