package net.dashmc.plots.plot.blocks;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.Debug;
import net.minecraft.server.v1_8_R3.BlockCarpet;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;

public class VirtualCarpetBlock extends VirtualBlock<BlockCarpet> {

	@Override
	public boolean interact(BlockCarpet block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return false;
	}

	@Override
	public int getDropData(IBlockData ibd) {
		int data = ((EnumColor) ibd.get(BlockCarpet.COLOR)).getColorIndex();
		Debug.log("getDropData called!: " + data);
		return data;
	}

	@Override
	public Class<BlockCarpet> getClazz() {
		return BlockCarpet.class;
	}

}
