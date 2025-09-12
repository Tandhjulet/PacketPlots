package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.BlockAir;
import net.minecraft.server.v1_8_R3.BlockPosition;

public class VirtualBlockAir extends VirtualBlock<BlockAir> {

	@Override
	public boolean shouldRemainAt(VirtualEnvironment env, BlockPosition pos) {
		return true;
	}

	@Override
	public Class<BlockAir> getClazz() {
		return BlockAir.class;
	}

}
