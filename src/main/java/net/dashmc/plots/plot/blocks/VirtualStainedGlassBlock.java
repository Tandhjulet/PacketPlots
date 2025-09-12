package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockStainedGlass;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualStainedGlassBlock extends VirtualBlock<BlockStainedGlass> {

	@Override
	public int getDropData(IBlockData ibd) {
		return ((EnumColor) ibd.get(BlockStainedGlass.COLOR)).getColorIndex();
	}

	@Override
	public Class<BlockStainedGlass> getClazz() {
		return BlockStainedGlass.class;
	}

}
