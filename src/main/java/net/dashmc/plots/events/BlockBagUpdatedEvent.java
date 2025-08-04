package net.dashmc.plots.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dashmc.plots.plot.BlockBag;

@Getter
@RequiredArgsConstructor
public class BlockBagUpdatedEvent extends Event implements Cancellable {
	@Setter
	private boolean cancelled = false;
	private final boolean isDeposit;
	private final BlockBag bag;

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
