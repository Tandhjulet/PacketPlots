package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockDoubleStepAbstract;
import net.minecraft.server.v1_8_R3.BlockStep;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockStep extends VirtualBlock<BlockStep> {

	@Override
	public int getDropData(IBlockData iblockdata) {
		return ((BlockDoubleStepAbstract.EnumStoneSlabVariant) iblockdata.get(BlockDoubleStepAbstract.VARIANT)).a();
	}

	@Override
	public Class<BlockStep> getClazz() {
		return BlockStep.class;
	}

}
