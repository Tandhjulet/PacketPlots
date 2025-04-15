package net.dashmc.plots.plot;

import java.util.HashMap;
import java.util.function.BiFunction;

import net.dashmc.plots.plot.items.VirtualBlockItem;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemStack;

public abstract class VirtualItem<T extends Item> {
	private static HashMap<Class<? extends Item>, VirtualItem<? extends Item>> virtualItems = new HashMap<>();

	public abstract boolean interactWith(T block, ItemStack itemStack, EntityHuman entityHuman,
			VirtualEnvironment environment, BlockPosition blockposition, EnumDirection enumdirection, float f, float f1,
			float f2);

	public abstract Class<T> getClazz();

	public void register() {
		virtualItems.put(getClazz(), this);
	}

	public static final <T extends Item> boolean interactWith(ItemStack itemStack, EntityHuman entityHuman,
			VirtualEnvironment environment, BlockPosition blockposition, EnumDirection enumdirection, float f, float f1,
			float f2) {
		return getAndRun(itemStack.getItem(), (BiFunction<VirtualItem<T>, T, Boolean>) (virtualItem, block) -> {
			if (virtualItem == null || block == null)
				return false;

			return virtualItem.interactWith(block, itemStack, entityHuman, environment, blockposition, enumdirection, f,
					f1, f2);
		});
	}

	@SuppressWarnings("unchecked")
	private static final <T extends Item, R> R getAndRun(Item item,
			BiFunction<VirtualItem<T>, T, R> callback) {
		if (item == null)
			return callback.apply(null, null);

		VirtualItem<? extends Item> virtualItem = virtualItems.get(item.getClass());
		if (virtualItem == null)
			return callback.apply(null, null);

		VirtualItem<T> typedVirtualBlock = (VirtualItem<T>) virtualItem;
		Class<T> clazz = (Class<T>) virtualItem.getClazz();
		T castBlock = clazz.cast(item);

		return callback.apply(typedVirtualBlock, castBlock);
	}

	static {
		new VirtualBlockItem().register();
	}

}
