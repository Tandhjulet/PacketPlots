package net.dashmc.plots.compatibility.compatibilities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.frep.vulcan.api.VulcanAPI;
import me.frep.vulcan.api.event.VulcanFlagEvent;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.compatibility.ICompatibility;

public class VulcanCompatibility implements ICompatibility {
	private VulcanAPI vulcan;
	private boolean activated = false;

	private VulcanAPI getVulcan() {
		if (this.vulcan != null)
			return vulcan;

		vulcan = VulcanAPI.Factory.getApi();
		return this.vulcan;
	}

	@Override
	public boolean shouldActivate() {
		return getVulcan() != null;
	}

	@Override
	public void activate(boolean forced) {
		if (this.activated)
			return;
		this.activated = true;
		Bukkit.getServer().getPluginManager().registerEvents(new VulcanFlagListener(), PacketPlots.getInstance());
	}

	public enum VulcanViolationType {
		COMBAT,
		MOVEMENT,
		AUTOCLICKER,
		PLAYER,
		TIMER,
		SCAFFOLD;

		public static VulcanViolationType from(String category) {
			return valueOf(category.toUpperCase());
		}
	}

	public class VulcanFlagListener implements Listener {
		@EventHandler
		public void onFlag(VulcanFlagEvent event) {
			if (event.isCancelled())
				return;

			VulcanViolationType violationType = VulcanViolationType.from(event.getCheck().getCategory());
			if (violationType != VulcanViolationType.MOVEMENT)
				return;

			Location loc = event.getPlayer().getLocation();
			if (!PacketPlots.getPlotConfig().getRegion().includes(loc))
				return;

			event.setCancelled(true);
		}
	}
}
