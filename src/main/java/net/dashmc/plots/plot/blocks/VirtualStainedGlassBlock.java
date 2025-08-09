package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockStainedGlass;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualStainedGlassBlock extends VirtualBlock<BlockStainedGlass> {

	@Override
	public boolean interact(BlockStainedGlass block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return false;
	}

	@Override
	public int getDropData(IBlockData ibd) {
		return ((EnumColor) ibd.get(BlockStainedGlass.COLOR)).getColorIndex();
	}

	@Override
	public Class<BlockStainedGlass> getClazz() {
		return BlockStainedGlass.class;
	}

}
