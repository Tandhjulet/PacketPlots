package net.dashmc.plots.plot.items;

import java.lang.reflect.Field;

import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.plot.VirtualItem;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockStepAbstract;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IBlockState;
import net.minecraft.server.v1_8_R3.ItemBlock;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.ItemStep;
import net.minecraft.server.v1_8_R3.BlockStepAbstract.EnumSlabHalf;

public class VirtualItemStep extends VirtualItem<ItemStep> {
	private static Field stepField;
	private static Field doubleField;

	@Override
	public boolean interactWith(ItemStack item, EntityHuman player, VirtualEnvironment environment, BlockPosition pos,
			EnumDirection direction, float cX, float cY, float cZ, boolean isBorderPlace) {
		if (item.count == 0)
			return false;
		else if (!environment.canPlace(pos, direction, item, player))
			return false;

		try {
			BlockStepAbstract blockStep = (BlockStepAbstract) stepField.get(item.getItem());
			IBlockData ibd = environment.getType(pos);
			if (ibd.getBlock() == blockStep) {
				IBlockState<?> ibs = blockStep.n();
				Comparable<?> comparable = ibd.get(ibs);

				EnumSlabHalf halfSlab = ibd.get(BlockStepAbstract.HALF);

				boolean isBottomPlace = direction == EnumDirection.UP && halfSlab == EnumSlabHalf.BOTTOM;
				boolean isTopPlace = direction == EnumDirection.DOWN && halfSlab == EnumSlabHalf.TOP;
				if ((isBottomPlace || isTopPlace) && comparable == blockStep.a(item)) {
					BlockStepAbstract fullBlock = (BlockStepAbstract) doubleField.get(item.getItem());

					@SuppressWarnings({ "rawtypes", "unchecked" })
					IBlockData updated = fullBlock.getBlockData().set((IBlockState) ibs, (Comparable) comparable);

					if (environment.setBlock(pos, updated, 3))
						item.count--;

					return true;
				}
			}

			ibd = environment.getType(pos.shift(direction));
			if (ibd.getBlock() == blockStep) {
				IBlockState<?> ibs = blockStep.n();
				Comparable<?> comparable = ibd.get(ibs);
				if (comparable == blockStep.a(item)) {
					BlockStepAbstract fullBlock = (BlockStepAbstract) doubleField.get(item.getItem());
					@SuppressWarnings({ "rawtypes", "unchecked" })
					IBlockData updated = fullBlock.getBlockData().set((IBlockState) ibs, (Comparable) comparable);

					if (environment.setBlock(pos.shift(direction), updated, 3))
						item.count--;

					return true;
				}
			}

		} catch (IllegalArgumentException | IllegalAccessException | NullPointerException e) {
			e.printStackTrace();
		}

		return VirtualItem.getVirtualItems().get(ItemBlock.class).interactWith(item, player, environment, pos,
				direction, cX, cY, cZ, isBorderPlace);
	}

	@Override
	public Class<ItemStep> getClazz() {
		return ItemStep.class;
	}

	static {
		try {
			stepField = ItemStep.class.getDeclaredField("b");
			stepField.setAccessible(true);

			doubleField = ItemStep.class.getDeclaredField("c");
			doubleField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}

}
