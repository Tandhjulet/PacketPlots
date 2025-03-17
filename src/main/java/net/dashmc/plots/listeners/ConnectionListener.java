package net.dashmc.plots.listeners;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.plot.VirtualEnvironment;

public class ConnectionListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		try {
			new VirtualEnvironment(player);
		} catch (IOException e) {
			Bukkit.getLogger()
					.severe("A severe error occured whilst loading the PacketPlot of " + player.getUniqueId());
			e.printStackTrace();
		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		VirtualEnvironment.get(player).stopVirtualization();
	}

	public static void register() {
		Bukkit.getPluginManager().registerEvents(new ConnectionListener(), PacketPlots.getInstance());
	}

}
