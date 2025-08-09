package net.dashmc.plots.plot.blocks;

import java.lang.reflect.InvocationTargetException;

import net.dashmc.plots.plot.VirtualBlock;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.MethodWrapper;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.BlockDoor;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Material;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldEvent;

public class VirtualBlockDoor extends VirtualBlock<BlockDoor> {

	@Override
	public boolean interact(BlockDoor block, VirtualEnvironment environment, BlockPosition pos,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		if (block.getMaterial() == Material.ORE)
			return true;

		BlockPosition lower = iblockdata.get(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER ? pos : pos.down();
		IBlockData door = pos.equals(lower) ? iblockdata : environment.getType(lower);

		if (door.getBlock() != block)
			return false;

		door = door.a(BlockDoor.OPEN);
		environment.setBlock(lower, door, 2);

		int eventCode = ((Boolean) iblockdata.get(BlockDoor.OPEN)).booleanValue() ? 1003 : 1006;
		environment.broadcastPacket(new PacketPlayOutWorldEvent(eventCode, pos, 0, false));
		return true;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(BlockDoor block, VirtualEnvironment env, BlockPosition pos,
			IBlockData state) {
		updateShape(block, env, pos);
		return super.getCollisionBoundingBox(block, env, pos, state);
	}

	private void updateShape(BlockDoor block, VirtualEnvironment env, BlockPosition pos) {
		int facing = getFacingData(env, pos);

		try {
			MethodWrapper<Void> updateBBMethod = new MethodWrapper<>(block, "k", int.class);
			updateBBMethod.call(facing);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
	}

	private static int getFacingData(VirtualEnvironment env, BlockPosition blockposition) {
		IBlockData iblockdata = env.getType(blockposition);
		int i = iblockdata.getBlock().toLegacyData(iblockdata);
		boolean flag = (i & 8) != 0;
		IBlockData iblockdata1 = env.getType(blockposition.down());
		int j = iblockdata1.getBlock().toLegacyData(iblockdata1);
		int k = flag ? j : i;
		IBlockData iblockdata2 = env.getType(blockposition.up());
		int l = iblockdata2.getBlock().toLegacyData(iblockdata2);
		int i1 = flag ? i : l;
		boolean flag1 = (i1 & 1) != 0;
		boolean flag2 = (i1 & 2) != 0;

		return (k & 7) | (flag ? 8 : 0) | (flag1 ? 16 : 0) | (flag2 ? 32 : 0);
	}

	@Override
	public Class<BlockDoor> getClazz() {
		return BlockDoor.class;
	}

}
