package net.dashmc.plots;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import eu.okaeri.commands.Commands;
import eu.okaeri.commands.bukkit.CommandsBukkit;
import eu.okaeri.commands.injector.CommandsInjector;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import eu.okaeri.injector.OkaeriInjector;
import lombok.Getter;
import net.dashmc.plots.commands.PlotCommand;
import net.dashmc.plots.config.PlotConfig;
import net.dashmc.plots.config.serializers.ChunkCoordPairSerializer;
import net.dashmc.plots.listeners.ConnectionListener;
import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.plot.VirtualEnvironment;

public class PacketPlots extends JavaPlugin {

	private @Getter static Commands commands;
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
		PacketInterceptor.register();

		// this.getCommand("plot").setExecutor(new PlotCommand());

		commands = CommandsBukkit.of(this);

		OkaeriInjector injector = OkaeriInjector.create(true);
		// commands.resultHandler(new BukkitCommandsResultHandler());
		commands.registerExtension(new CommandsInjector(injector));
		injector.registerInjectable("commands", commands);

		commands.registerCommand(PlotCommand.class);
	}

	@Override
	public void onDisable() {
		try {
			commands.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (VirtualEnvironment env : VirtualEnvironment.getActive()) {
			env.save();
		}

	}

	@Override
	public FileConfiguration getConfig() {
		throw new UnsupportedOperationException();
	}

}