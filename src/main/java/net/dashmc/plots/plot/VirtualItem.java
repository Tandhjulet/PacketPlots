package net.dashmc.plots.plot;

import java.util.HashMap;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
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

	public void register() {
		virtualItems.put(getClazz(), this);
	}

	static {

	}

}
