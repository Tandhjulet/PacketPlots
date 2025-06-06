package net.dashmc.plots.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dashmc.plots.plot.VirtualEnvironment;

@Getter
@RequiredArgsConstructor
public class VirtualBlockDamageEvent extends Event implements Cancellable {
	@Setter
	private boolean cancelled = false;
	private final Location location;
	private final Player player;
	private final ItemStack itemInHand;
	private final boolean instaBreak;
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
