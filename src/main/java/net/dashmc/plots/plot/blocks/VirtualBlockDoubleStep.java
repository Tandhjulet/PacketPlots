package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockDoubleStep;
import net.minecraft.server.v1_8_R3.BlockDoubleStepAbstract;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockDoubleStep extends VirtualBlock<BlockDoubleStep> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockDoubleStepAbstract.EnumStoneSlabVariant) iblockdata.get(BlockDoubleStepAbstract.VARIANT)).a();
	}

	@Override
	public Class<BlockDoubleStep> getClazz() {
		return BlockDoubleStep.class;
	}

}
