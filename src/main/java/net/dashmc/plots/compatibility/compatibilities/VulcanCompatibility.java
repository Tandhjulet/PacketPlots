package net.dashmc.plots.compatibility.compatibilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import me.frep.vulcan.api.event.VulcanFlagEvent;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.compatibility.ICompatibility;
import net.dashmc.plots.utils.Debug;

public class VulcanCompatibility implements ICompatibility {

	@Override
	public boolean shouldActivate() {
		return Bukkit.getPluginManager().getPlugin("Vulcan") != null;
	}

	@Override
	public void activate(boolean forced) {
		updateVulcanConfig();

		Bukkit.getPluginManager().registerEvents(new VulcanFlagListener(), PacketPlots.getInstance());

	}

	private void updateVulcanConfig() {
		Plugin vulcan = Bukkit.getPluginManager().getPlugin("Vulcan");

		try {
			YamlConfiguration vulcanConfig = new YamlConfiguration();
			File confFile = new File(vulcan.getDataFolder(), "config.yml");
			FileInputStream stream = new FileInputStream(confFile);
			vulcanConfig.load(new InputStreamReader(stream, Charsets.UTF_8));

			Boolean isGhostBlocksEnabled = vulcanConfig.getBoolean("ghost-blocks-fix.enabled");
			Boolean isAPIEnabled = vulcanConfig.getBoolean("settings.enable-api");
			Debug.log("Is ghost blocks enabled? " + isGhostBlocksEnabled);
			Debug.log("Is api enabled? " + isAPIEnabled);

			boolean shouldReload = false;

			if (isGhostBlocksEnabled) {
				Bukkit.getLogger().warning("Ghost blocks fix was enabled... disabling for compatibility...");

				vulcanConfig.set("ghost-blocks-fix.enabled", false);
				vulcanConfig.save(confFile);

				shouldReload = true;
			}

			if (!isAPIEnabled) {
				Bukkit.getLogger().warning("The Vulcan API was disabled... enabling for compatibility...");

				vulcanConfig.set("settings.enable-api", true);
				vulcanConfig.save(confFile);

				shouldReload = true;
			}

			if (!shouldReload)
				return;

			Bukkit.getLogger().warning("Done! Reloading vulcan...");
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "vulcan reload");
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
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

	private static class VulcanFlagListener implements Listener {

		@EventHandler
		public void onFlag(VulcanFlagEvent ev) {
			String category = ev.getCheck().getCategory();
			VulcanViolationType violationType = VulcanViolationType.from(category);
			if (violationType != VulcanViolationType.MOVEMENT)
				return;

			Location playerLocation = ev.getPlayer().getLocation();
			if (!PacketPlots.getPlotConfig().getRegion().includesWithBuffer(playerLocation))
				return;

			Debug.log("cancelled vulcan flag due to being in virtualized region (checked w/ buffer)!");
			ev.setCancelled(true);
		}

	}
}
