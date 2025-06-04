package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.BlockAir;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualBlockAir extends VirtualBlock<BlockAir> {

	@Override
	public boolean interact(BlockAir block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return false;
	}

	@Override
	public boolean shouldRemainAt(VirtualEnvironment env, BlockPosition pos) {
		return true;
	}

	@Override
	public Class<BlockAir> getClazz() {
		return BlockAir.class;
	}

}
