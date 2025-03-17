package net.dashmc.plots.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.plot.VirtualEnvironment;

public class ConnectionListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		new VirtualEnvironment(player);

	}

	public static void register() {
		Bukkit.getPluginManager().registerEvents(new ConnectionListener(), PacketPlots.getInstance());
	}

}
