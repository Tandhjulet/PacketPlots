package net.dashmc.plots.compatibility.compatibilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import net.dashmc.plots.compatibility.ICompatibility;
import net.dashmc.plots.utils.Debug;

public class VulcanCompatibility implements ICompatibility {

	@Override
	public boolean shouldActivate() {
		return Bukkit.getPluginManager().getPlugin("Vulcan") != null;
	}

	@Override
	public void activate(boolean forced) {
		Plugin vulcan = Bukkit.getPluginManager().getPlugin("Vulcan");

		try {
			YamlConfiguration vulcanConfig = new YamlConfiguration();
			File confFile = new File(vulcan.getDataFolder(), "config.yml");
			FileInputStream stream = new FileInputStream(confFile);
			vulcanConfig.load(new InputStreamReader(stream, Charsets.UTF_8));

			Boolean isGhostBlocksEnabled = vulcanConfig.getBoolean("ghost-blocks-fix.enabled");
			Debug.log("Is ghost blocks enabled? " + isGhostBlocksEnabled);

			if (!isGhostBlocksEnabled)
				return;

			Bukkit.getLogger().warning("Ghost blocks fix was enabled... disabling for compatibility...");

			vulcanConfig.set("ghost-blocks-fix.enabled", false);
			vulcanConfig.save(confFile);

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
}
