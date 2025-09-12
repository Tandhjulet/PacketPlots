package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockStainedGlassPane;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualStainedGlassPaneBlock extends VirtualBlock<BlockStainedGlassPane> {

	@Override
	public int getDropData(IBlockData ibd) {
		return ((EnumColor) ibd.get(BlockStainedGlassPane.COLOR)).getColorIndex();
	}

	@Override
	public Class<BlockStainedGlassPane> getClazz() {
		return BlockStainedGlassPane.class;
	}

}
