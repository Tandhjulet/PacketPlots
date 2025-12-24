package net.dashmc.plots.listeners;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.player.VirtualPlayerInteractManager;
import net.dashmc.plots.plot.Environment;
import net.dashmc.plots.plot.IEnvironment;
import net.dashmc.plots.plot.VirtualConnection;
import net.dashmc.plots.plot.CuboidEnvironment;
import net.dashmc.plots.plot.data.BlockBag;
import net.dashmc.plots.utils.Debug;

public class ConnectionListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		try {
			Debug.log(((CraftPlayer) player).getHandle().playerInteractManager.getClass().getName());
			VirtualPlayerInteractManager.inject(player);

			new CuboidEnvironment(player);
		} catch (IOException | IllegalArgumentException | IllegalAccessException e) {
			Bukkit.getLogger()
					.severe("A severe error occured whilst loading the PacketPlot of " + player.getUniqueId());
			e.printStackTrace();
		}

	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		BlockBag.removeBlockBag(player);

		VirtualConnection connection = VirtualConnection.get(((CraftPlayer) player).getHandle());
		if (connection != null)
			connection.close();

		IEnvironment environment = Environment.get(player);
		if (environment != null)
			environment.close();
	}

	public static void register() {
		Bukkit.getPluginManager().registerEvents(new ConnectionListener(), PacketPlots.getInstance());
	}

}
