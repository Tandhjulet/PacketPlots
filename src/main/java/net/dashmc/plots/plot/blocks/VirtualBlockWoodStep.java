package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockWood;
import net.minecraft.server.v1_8_R3.BlockWoodStep;
import net.minecraft.server.v1_8_R3.BlockWoodenStep;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockWoodStep extends VirtualBlock<BlockWoodStep> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockWood.EnumLogVariant) iblockdata.get(BlockWoodenStep.VARIANT)).a();
	}

	@Override
	public Class<BlockWoodStep> getClazz() {
		return BlockWoodStep.class;
	}

}
