package net.dashmc.plots.plot;

import java.util.HashMap;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.dashmc.plots.events.VirtualBlockPlaceEvent;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockContainer;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemStack;

public abstract class VirtualItem<T extends Item> {
	private static HashMap<Class<? extends Item>, VirtualItem<? extends Item>> virtualItems = new HashMap<>();

	/**
	 * 
	 * @param item        the held item
	 * @param player      the player that tried interacting
	 * @param environment the environment in which the player tried interacting
	 * @param pos         the position at which the user tried interacting
	 * @param direction   the direction of the block
	 * @param cX          the position of the crosshair on the block
	 * @param cY
	 * @param cZ
	 * @return
	 */
	public abstract boolean interactWith(ItemStack item, EntityHuman player, VirtualEnvironment environment,
			BlockPosition pos,
			EnumDirection direction, float cX, float cY, float cZ);

	public abstract Class<T> getClazz();

	@SuppressWarnings("unchecked")
	private static final <T extends Item, R> R getAndRun(Item item,
			BiFunction<VirtualItem<T>, T, R> callback) {
		if (item == null)
			return callback.apply(null, null);

		VirtualItem<? extends Item> virtualItem = virtualItems.get(item.getClass());
		if (virtualItem == null)
			return callback.apply(null, null);
		VirtualItem<T> typedVirtualItem = (VirtualItem<T>) virtualItem;

		Class<T> clazz = (Class<T>) virtualItem.getClazz();
		if (clazz == null)
			return callback.apply(typedVirtualItem, null);

		T castItem = clazz.cast(item);
		return callback.apply(typedVirtualItem, castItem);
	}

	public static final <T extends Item> boolean placeItem(ItemStack itemStack, EntityHuman entityhuman,
			VirtualEnvironment env, BlockPosition clicked, EnumDirection dir, float cX, float cY, float cZ) {
		Player player = (Player) entityhuman;

		return getAndRun(itemStack.getItem(), (BiFunction<VirtualItem<T>, T, Boolean>) (virtualItem, actualItem) -> {
			if (actualItem == null || virtualItem == null)
				return false;

			int data = itemStack.getData();
			int count = itemStack.count;
			BlockPosition placedAt = clicked.shift(dir);
			Location bukkitPlacedAt = new Location(player.getWorld(), placedAt.getX(), placedAt.getY(),
					placedAt.getZ());

			IBlockData prevBlockData = env.getType(placedAt);
			boolean flag = virtualItem.interactWith(itemStack, entityhuman, env, clicked, dir, cX, cY, cZ);

			int newData = itemStack.getData();
			int newCount = itemStack.count;
			itemStack.count = count;
			itemStack.setData(data);

			if (flag) {
				IBlockData newBlockData = env.getType(placedAt);

				@SuppressWarnings("deprecation")
				Material mat = Material.getMaterial(Block.getId(newBlockData.getBlock()));

				VirtualBlockPlaceEvent ev = new VirtualBlockPlaceEvent(player, bukkitPlacedAt, mat);
				Bukkit.getPluginManager().callEvent(ev);

				if (ev.isCancelled()) {
					env.setBlock(placedAt, prevBlockData, 3);
					return false;
				} else {
					if (itemStack.count == count && itemStack.getData() == data) {
						itemStack.setData(newData);
						itemStack.count = newCount;
					}

					if (!(newBlockData instanceof BlockContainer)) {
						VirtualBlock.onPlace(env, clicked, newBlockData);
					}

				}
			}

			return flag;
		});
	}

	public void register() {
		virtualItems.put(getClazz(), this);
	}

	static {

	}

}
