package net.dashmc.plots.compatibility.compatibilities;

import org.bukkit.Bukkit;

import net.dashmc.plots.compatibility.ICompatibility;

public class ChunkMapSendCompatibility implements ICompatibility {

	@Override
	public boolean shouldActivate() {
		return Bukkit.getServer().getPluginManager().getPlugin("packetevents") != null;
	}

	@Override
	public void activate(boolean forced) {
	}

}
