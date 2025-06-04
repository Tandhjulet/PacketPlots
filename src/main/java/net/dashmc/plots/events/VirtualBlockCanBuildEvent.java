package net.dashmc.plots.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dashmc.plots.plot.VirtualEnvironment;

@Getter
@RequiredArgsConstructor
public class VirtualBlockCanBuildEvent extends Event {
	private final boolean buildable;
	private final Location location;
	private final Material material;
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
