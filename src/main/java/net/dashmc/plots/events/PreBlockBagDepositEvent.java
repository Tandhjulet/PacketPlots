package net.dashmc.plots.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dashmc.plots.plot.BlockBag;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.ItemStack;

@RequiredArgsConstructor
@Getter
public class PreBlockBagDepositEvent extends Event implements Cancellable {
	@Setter
	private boolean cancelled = false;
	private final ItemStack[] items;
	private final BlockBag to;
	private final EntityPlayer player;

	public PreBlockBagDepositEvent(ItemStack[] items, BlockBag to) {
		this.items = items;
		this.to = to;
		this.player = to.getPlayer();
	}

	public PreBlockBagDepositEvent(ItemStack item, BlockBag to) {
		this(new ItemStack[] { item }, to);
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
