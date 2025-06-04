package net.dashmc.plots.plot;

import java.util.HashMap;
import java.util.function.BiFunction;

import net.dashmc.plots.plot.blocks.VirtualBlockAir;
import net.dashmc.plots.plot.blocks.VirtualChestBlock;
import net.dashmc.plots.plot.blocks.VirtualDirtBlock;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.ItemStack;

public abstract class VirtualBlock<T extends Block> {
	private static HashMap<Class<? extends Block>, VirtualBlock<? extends Block>> virtualBlocks = new HashMap<>();

	public abstract boolean interact(T block, VirtualEnvironment environment, BlockPosition blockposition,
			IBlockData iblockdata,
			EntityHuman entityhuman, EnumDirection enumdirection, float f, float f1, float f2);

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

	public boolean shouldRemainAt(VirtualEnvironment env, BlockPosition pos) {
		return false;
	}

	public void postPlace(T block, VirtualEnvironment environment, BlockPosition blockposition, IBlockData iblockdata,
			EntityLiving entityliving, ItemStack itemstack) {
	}

	public abstract Class<T> getClazz();

	public void register() {
		virtualBlocks.put(getClazz(), this);
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
				return false;
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
		new VirtualDirtBlock().register();
	}

}
