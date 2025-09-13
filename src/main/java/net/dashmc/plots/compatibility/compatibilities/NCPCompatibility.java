package net.dashmc.plots.compatibility.compatibilities;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.compatibility.CompatibilityLoader;
import net.dashmc.plots.events.EnvironmentEnterExit;
import net.minecraft.server.v1_8_R3.EntityPlayer;

public class NCPCompatibility extends CompatibilityLoader {

	@Override
	public boolean shouldActivate() {
		return Bukkit.getPluginManager().getPlugin("NoCheatPlus") != null;
	}

	@Override
	public void activate(boolean forced) {
		Bukkit.getPluginManager().registerEvents(new EnvironmentListener(), PacketPlots.getInstance());
	}

	private static class EnvironmentListener implements Listener {

		@EventHandler
		public void onEnterExit(EnvironmentEnterExit ev) {
			EntityPlayer player = ev.getConnection().getPlayer();
			if (ev.isEnter()) {
				NCPExemptionManager.exemptPermanently(player.getUniqueID());
			} else
				NCPExemptionManager.unexempt(player.getUniqueID());
		}

	}
}
