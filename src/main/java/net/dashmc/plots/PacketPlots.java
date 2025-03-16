package net.dashmc.plots;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import lombok.Getter;
import net.dashmc.plots.config.PlotConfig;
import net.dashmc.plots.config.serializers.ChunkCoordPairSerializer;

public class PacketPlots extends JavaPlugin {

	private @Getter static PacketPlots instance;
	private @Getter static PlotConfig plotConfig;

	@Override
	public void onEnable() {
		instance = this;

		plotConfig = ConfigManager.create(PlotConfig.class, (conf) -> {
			conf.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
			conf.withSerdesPack(reg -> {
				reg.register(new ChunkCoordPairSerializer());
			});
			conf.withBindFile(new File(this.getDataFolder(), "config.yml"));
			conf.saveDefaults();
			conf.load(true);
		});
	}

	@Override
	public FileConfiguration getConfig() {
		throw new UnsupportedOperationException();
	}

}