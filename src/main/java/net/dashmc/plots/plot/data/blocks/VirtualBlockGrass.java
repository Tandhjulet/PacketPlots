package net.dashmc.plots.plot.data.blocks;

import net.dashmc.plots.plot.data.VirtualBlock;
import net.minecraft.server.v1_8_R3.BlockGrass;

public class VirtualBlockGrass extends VirtualBlock<BlockGrass> {

	@Override
	public Class<BlockGrass> getClazz() {
		return BlockGrass.class;
	}

}
