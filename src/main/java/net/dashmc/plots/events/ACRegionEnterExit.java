package net.dashmc.plots.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dashmc.plots.plot.VirtualConnection;

@Getter
@RequiredArgsConstructor
public class ACRegionEnterExit extends Event {
	private final VirtualConnection connection;
	private final boolean enter;

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}