package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockStainedGlassPane;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualStainedGlassPaneBlock extends VirtualBlock<BlockStainedGlassPane> {

	@Override
	public boolean interact(BlockStainedGlassPane block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return false;
	}

	@Override
	public int getDropData(IBlockData ibd) {
		return ((EnumColor) ibd.get(BlockStainedGlassPane.COLOR)).getColorIndex();
	}

	@Override
	public Class<BlockStainedGlassPane> getClazz() {
		return BlockStainedGlassPane.class;
	}

}
