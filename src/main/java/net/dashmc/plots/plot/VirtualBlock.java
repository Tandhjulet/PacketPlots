package net.dashmc.plots.plot;

import java.util.HashMap;
import java.util.function.BiFunction;

import net.dashmc.plots.plot.blocks.VirtualBlockAir;
import net.dashmc.plots.plot.blocks.VirtualBlockDoor;
import net.dashmc.plots.plot.blocks.VirtualBlockDoubleStep;
import net.dashmc.plots.plot.blocks.VirtualBlockGrass;
import net.dashmc.plots.plot.blocks.VirtualBlockLeaves1;
import net.dashmc.plots.plot.blocks.VirtualBlockLeaves2;
import net.dashmc.plots.plot.blocks.VirtualBlockLog1;
import net.dashmc.plots.plot.blocks.VirtualBlockLog2;
import net.dashmc.plots.plot.blocks.VirtualBlockStep;
import net.dashmc.plots.plot.blocks.VirtualBlockStone;
import net.dashmc.plots.plot.blocks.VirtualBlockTrapDoor;
import net.dashmc.plots.plot.blocks.VirtualBlockWood;
import net.dashmc.plots.plot.blocks.VirtualBlockWoodStep;
import net.dashmc.plots.plot.blocks.VirtualCarpetBlock;
import net.dashmc.plots.plot.blocks.VirtualChestBlock;
import net.dashmc.plots.plot.blocks.VirtualEnderChestBlock;
import net.dashmc.plots.plot.blocks.VirtualFenceGateBlock;
import net.dashmc.plots.plot.blocks.VirtualSkullBlock;
import net.dashmc.plots.plot.blocks.VirtualStainedGlassBlock;
import net.dashmc.plots.plot.blocks.VirtualStainedGlassPaneBlock;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.TileEntity;

public abstract class VirtualBlock<T extends Block> {
	private static HashMap<Class<? extends Block>, VirtualBlock<? extends Block>> virtualBlocks = new HashMap<>();

	public boolean interact(T block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata, EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return false;
	}

	public void onPlace(T block, VirtualEnvironment environment, BlockPosition pos, IBlockData blockData) {
	}

	public void remove(T block, VirtualEnvironment environment, BlockPosition pos, IBlockData blockData) {
	}

	public boolean canPlace(T block, VirtualEnvironment environment, BlockPosition pos, EnumDirection direction,
			ItemStack itemStack) {
		return canPlace(block, environment, pos, direction);
	}

	public boolean canPlace(T block, VirtualEnvironment environment, BlockPosition pos, EnumDirection direction) {
		return canPlace(block, environment, pos);
	}

	public boolean canPlace(T block, VirtualEnvironment environment, BlockPosition pos) {
		return environment.getType(pos).getBlock().getMaterial().isReplaceable();
	}

	public void onRelatedUpdated(T block, VirtualEnvironment env, BlockPosition pos, BlockBag bag,
			IBlockData newBlockData) {
	}

	public void onBlockHarvested(T block, VirtualEnvironment environment, BlockPosition pos, IBlockData data,
			BlockBag bag,
			TileEntity tile) {
		bag.add(new ItemStack(getItemType(block, data), 1, getDropData(data)));
	};

	public Item getItemType(T block, IBlockData ibd) {
		return Item.getItemOf(block);
	}

	public int getDropData(VirtualEnvironment env, BlockPosition pos) {
		return getDropData(env.getType(pos));
	}

	public int getDropData(IBlockData data) {
		return 0;
	}

	public AxisAlignedBB getCollisionBoundingBox(T block, VirtualEnvironment env, BlockPosition pos, IBlockData state) {
		double minX = block.B();
		double minY = block.D();
		double minZ = block.F();

		double maxX = block.C();
		double maxY = block.E();
		double maxZ = block.G();

		return new AxisAlignedBB((double) pos.getX() + minX, (double) pos.getY() + minY, (double) pos.getZ() + minZ,
				(double) pos.getX() + maxX, (double) pos.getY() + maxY, (double) pos.getZ() + maxZ);
	}

	public boolean shouldRemainAt(VirtualEnvironment env, BlockPosition pos) {
		return false;
	}

	public void postPlace(T block, VirtualEnvironment environment, BlockPosition blockposition, IBlockData iblockdata,
			EntityLiving entityliving, ItemStack itemstack) {
	}

	public void postBreak(T block, VirtualEnvironment environment, BlockPosition blockposition, IBlockData iblockdata) {
	}

	public abstract Class<T> getClazz();

	public void register() {
		virtualBlocks.put(getClazz(), this);
	}

	public static final <T extends Block> void notify(Block block, VirtualEnvironment env, BlockPosition pos,
			BlockBag bag,
			IBlockData newBlockData) {
		getAndRun(block, (BiFunction<VirtualBlock<T>, T, Void>) (virtualBlock, actualBlock) -> {
			if (virtualBlock == null || actualBlock == null)
				return null;

			virtualBlock.onRelatedUpdated(actualBlock, env, pos, bag, newBlockData);
			return null;
		});
	}

	public static final <T extends Block> AxisAlignedBB getBoundingBox(Block block, VirtualEnvironment env,
			BlockPosition pos, IBlockData state) {
		return getAndRun(block, (BiFunction<VirtualBlock<T>, T, AxisAlignedBB>) (virtualBlock, actualBlock) -> {
			if (virtualBlock == null || actualBlock == null)
				return block.a((net.minecraft.server.v1_8_R3.World) null, pos, state);

			return virtualBlock.getCollisionBoundingBox(actualBlock, env, pos, state);
		});
	}

	public static final <T extends Block> boolean interact(VirtualEnvironment environment,
			BlockPosition blockposition,
			IBlockData iblockdata,
			EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2) {
		return getAndRun(iblockdata, (BiFunction<VirtualBlock<T>, T, Boolean>) (virtualBlock, block) -> {
			if (virtualBlock == null || block == null)
				return false;
			return virtualBlock.interact(
					block, environment, blockposition, iblockdata, entityhuman,
					enumdirection, f, f1, f2);
		});
	}

	public static final <T extends Block> void harvestBlock(Block block, IBlockData data,
			VirtualEnvironment environment,
			BlockPosition pos, BlockBag bag, TileEntity tile) {
		getAndRun(block, (BiFunction<VirtualBlock<T>, T, Void>) (virtualBlock, actualBlock) -> {
			if (virtualBlock == null || block == null) {
				bag.add(Item.getItemOf(block));
				return null;
			}
			virtualBlock.onBlockHarvested(actualBlock, environment, pos, data, bag, tile);
			return null;
		});
	}

	public static final <T extends Block> boolean shouldRemainAt(Block block, VirtualEnvironment env,
			BlockPosition position) {
		return getAndRun(block, (BiFunction<VirtualBlock<T>, T, Boolean>) (virtualBlock, actualBlock) -> {
			if (virtualBlock == null || block == null)
				return false;
			return virtualBlock.shouldRemainAt(env, position);
		});
	}

	public static final <T extends Block> boolean mayPlace(Block toPlace, VirtualEnvironment environment,
			BlockPosition pos, EnumDirection direction, ItemStack itemStack) {
		return getAndRun(toPlace, (BiFunction<VirtualBlock<T>, T, Boolean>) (virtualBlock, block) -> {
			if (virtualBlock == null || block == null)
				return environment.getType(pos).getBlock().getMaterial().isReplaceable();
			return virtualBlock.canPlace(block, environment, pos, direction, itemStack);
		});
	}

	public static final <T extends Block> void postPlace(Block placedBlock, VirtualEnvironment environment,
			BlockPosition pos,
			IBlockData blockData, EntityHuman entityHuman, ItemStack itemStack) {
		getAndRun(placedBlock, (BiFunction<VirtualBlock<T>, T, Void>) (virtualBlock, block) -> {
			if (virtualBlock == null || block == null)
				return null;
			virtualBlock.postPlace(block, environment, pos, blockData, (EntityLiving) entityHuman, itemStack);
			return null;
		});
	}

	public static final <T extends Block> void remove(VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata) {
		getAndRun(iblockdata, (BiFunction<VirtualBlock<T>, T, Void>) (virtualBlock, block) -> {
			if (virtualBlock == null || block == null)
				return null;
			virtualBlock.remove(block, environment, blockposition, iblockdata);
			return null;
		});
	}

	public static final <T extends Block> void onPlace(VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata) {
		getAndRun(iblockdata, (BiFunction<VirtualBlock<T>, T, Void>) (virtualBlock, block) -> {
			if (virtualBlock == null || block == null)
				return null;
			virtualBlock.onPlace(block, environment, blockposition, iblockdata);
			return null;
		});
	}

	private static final <T extends Block, R> R getAndRun(IBlockData blockData,
			BiFunction<VirtualBlock<T>, T, R> callback) {
		Block block = blockData.getBlock();
		return getAndRun(block, callback);
	}

	@SuppressWarnings("unchecked")
	private static final <T extends Block, R> R getAndRun(Block block,
			BiFunction<VirtualBlock<T>, T, R> callback) {
		if (block == null)
			return callback.apply(null, null);

		VirtualBlock<? extends Block> virtualBlock = virtualBlocks.get(block.getClass());
		if (virtualBlock == null)
			return callback.apply(null, null);
		VirtualBlock<T> typedVirtualBlock = (VirtualBlock<T>) virtualBlock;

		Class<T> clazz = (Class<T>) virtualBlock.getClazz();
		if (clazz == null)
			return callback.apply(typedVirtualBlock, null);

		T castBlock = clazz.cast(block);

		return callback.apply(typedVirtualBlock, castBlock);
	}

	static {
		new VirtualBlockAir().register();
		new VirtualChestBlock().register();
		new VirtualCarpetBlock().register();
		new VirtualBlockDoor().register();
		new VirtualEnderChestBlock().register();
		new VirtualFenceGateBlock().register();
		new VirtualStainedGlassBlock().register();
		new VirtualStainedGlassPaneBlock().register();
		new VirtualSkullBlock().register();
		new VirtualBlockLeaves1().register();
		new VirtualBlockLeaves2().register();
		new VirtualBlockLog1().register();
		new VirtualBlockLog2().register();
		new VirtualBlockWood().register();
		new VirtualBlockStone().register();
		new VirtualBlockWoodStep().register();
		new VirtualBlockStep().register();
		new VirtualBlockDoubleStep().register();
		new VirtualBlockGrass().register();
		new VirtualBlockTrapDoor().register();
	}

}
