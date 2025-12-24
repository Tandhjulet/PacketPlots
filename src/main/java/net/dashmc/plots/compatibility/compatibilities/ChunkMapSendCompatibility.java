package net.dashmc.plots.compatibility.compatibilities;

import org.bukkit.Bukkit;

import net.dashmc.plots.compatibility.CompatibilityLoader;

public class ChunkMapSendCompatibility extends CompatibilityLoader {

	@Override
	public boolean shouldActivate() {
		return Bukkit.getServer().getPluginManager().getPlugin("packetevents") != null;
	}

	/**
	 * doesnt actively change anything - is depended upon
	 * 
	 * @see src\main\java\net\dashmc\plots\plot\VirtualConnection.java#L143
	 */
	@Override
	public void activate(boolean forced) {
	}

}
