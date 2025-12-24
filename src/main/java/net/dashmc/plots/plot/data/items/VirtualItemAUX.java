package net.dashmc.plots.plot.data.items;

import net.dashmc.plots.plot.IEnvironment;
import net.dashmc.plots.plot.data.VirtualItem;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.ItemBlock;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.ItemWithAuxData;

public class VirtualItemAUX extends VirtualItem<ItemWithAuxData> {

	@Override
	public boolean interactWith(ItemStack item, EntityHuman player, IEnvironment environment, BlockPosition pos,
			EnumDirection direction, float cX, float cY, float cZ, boolean isBorderPlace) {
		return VirtualItem.getVirtualItems().get(ItemBlock.class).interactWith(item, player, environment, pos,
				direction, cX, cY, cZ, isBorderPlace);
	}

	@Override
	public Class<ItemWithAuxData> getClazz() {
		return ItemWithAuxData.class;
	}

}
