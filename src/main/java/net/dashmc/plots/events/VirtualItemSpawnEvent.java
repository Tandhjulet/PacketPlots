package net.dashmc.plots.events;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dashmc.plots.plot.VirtualEnvironment;

@Getter
@RequiredArgsConstructor
public class VirtualItemSpawnEvent extends Event implements Cancellable {
	@Setter
	private boolean cancelled = false;
	private final Location location;
	private final Entity entity;
	private final VirtualEnvironment environment;

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
