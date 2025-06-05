package net.dashmc.plots;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import lombok.Getter;
import net.dashmc.plots.commands.CommandPlot;
import net.dashmc.plots.config.PlotConfig;
import net.dashmc.plots.config.serializers.ChunkCoordPairSerializer;
import net.dashmc.plots.listeners.ConnectionListener;
import net.dashmc.plots.packets.PacketModifier;
import net.dashmc.plots.plot.VirtualEnvironment;

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

		ConnectionListener.register();
		PacketModifier.register();

		this.getCommand("plot").setExecutor(new CommandPlot());
	}

	@Override
	public void onDisable() {

		for (VirtualEnvironment env : VirtualEnvironment.getActive()) {
			env.save();
		}

	}

	@Override
	public FileConfiguration getConfig() {
		throw new UnsupportedOperationException();
	}

}