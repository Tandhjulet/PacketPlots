package net.dashmc.plots.plot.blocks;

import java.util.Iterator;

import org.bukkit.Bukkit;

import lombok.AllArgsConstructor;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockChest;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;

@AllArgsConstructor
public class ChestHelper {
	private final Block block;

	public void onPlace(VirtualEnvironment world, BlockPosition blockposition, IBlockData iblockdata) {
		this.e(world, blockposition, iblockdata);
		Iterator<EnumDirection> iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

		while (iterator.hasNext()) {
			EnumDirection enumdirection = (EnumDirection) iterator.next();
			BlockPosition blockposition1 = blockposition.shift(enumdirection);
			IBlockData iblockdata1 = world.getType(blockposition1);

			if (iblockdata1.getBlock() == this.block) {
				this.e(world, blockposition1, iblockdata1);
			}
		}
	}

	public IBlockData e(VirtualEnvironment world, BlockPosition blockposition, IBlockData iblockdata) {
		Bukkit.getLogger().info("blockPosition: " + blockposition);
		Bukkit.getLogger().info("blockPosition: " + world);
		IBlockData iblockdata1 = world.getType(blockposition.north());
		IBlockData iblockdata2 = world.getType(blockposition.south());
		IBlockData iblockdata3 = world.getType(blockposition.west());
		IBlockData iblockdata4 = world.getType(blockposition.east());
		EnumDirection enumdirection = (EnumDirection) iblockdata.get(BlockChest.FACING);
		Block block = iblockdata1.getBlock();
		Block block1 = iblockdata2.getBlock();
		Block block2 = iblockdata3.getBlock();
		Block block3 = iblockdata4.getBlock();

		if (block != this.block && block1 != this.block) {
			boolean flag = block.o();
			boolean flag1 = block1.o();

			if (block2 == this.block || block3 == this.block) {
				BlockPosition blockposition1 = block2 == this.block ? blockposition.west() : blockposition.east();
				IBlockData iblockdata5 = world.getType(blockposition1.north());
				IBlockData iblockdata6 = world.getType(blockposition1.south());

				enumdirection = EnumDirection.SOUTH;
				EnumDirection enumdirection1;

				if (block2 == this.block) {
					enumdirection1 = (EnumDirection) iblockdata3.get(BlockChest.FACING);
				} else {
					enumdirection1 = (EnumDirection) iblockdata4.get(BlockChest.FACING);
				}

				if (enumdirection1 == EnumDirection.NORTH) {
					enumdirection = EnumDirection.NORTH;
				}

				Block block4 = iblockdata5.getBlock();
				Block block5 = iblockdata6.getBlock();

				if ((flag || block4.o()) && !flag1 && !block5.o()) {
					enumdirection = EnumDirection.SOUTH;
				}

				if ((flag1 || block5.o()) && !flag && !block4.o()) {
					enumdirection = EnumDirection.NORTH;
				}
			}
		} else {
			BlockPosition blockposition2 = block == this.block ? blockposition.north() : blockposition.south();
			IBlockData iblockdata7 = world.getType(blockposition2.west());
			IBlockData iblockdata8 = world.getType(blockposition2.east());

			enumdirection = EnumDirection.EAST;
			EnumDirection enumdirection2;

			if (block == this.block) {
				enumdirection2 = (EnumDirection) iblockdata1.get(BlockChest.FACING);
			} else {
				enumdirection2 = (EnumDirection) iblockdata2.get(BlockChest.FACING);
			}

			if (enumdirection2 == EnumDirection.WEST) {
				enumdirection = EnumDirection.WEST;
			}

			Block block6 = iblockdata7.getBlock();
			Block block7 = iblockdata8.getBlock();

			if ((block2.o() || block6.o()) && !block3.o() && !block7.o()) {
				enumdirection = EnumDirection.EAST;
			}

			if ((block3.o() || block7.o()) && !block2.o() && !block6.o()) {
				enumdirection = EnumDirection.WEST;
			}
		}

		iblockdata = iblockdata.set(BlockChest.FACING, enumdirection);
		world.setBlock(blockposition, iblockdata, 3);
		return iblockdata;
	}
}
