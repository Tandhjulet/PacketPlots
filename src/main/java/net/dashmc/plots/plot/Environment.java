package net.dashmc.plots.plot;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import org.bukkit.entity.Player;

import net.dashmc.plots.PacketPlots;
import net.minecraft.server.v1_8_R3.EntityPlayer;

public abstract class Environment implements IEnvironment {
	protected static final File DATA_DIRECTORY = new File(PacketPlots.getInstance().getDataFolder(), "data");

	private static final HashMap<Player, IEnvironment> environments = new HashMap<>();

	public static Collection<IEnvironment> getActive() {
		return environments.values();
	}

	public static IEnvironment get(Player player) {
		return environments.get(player);
	}

	protected IEnvironment createEnvironment(Player player) {
		environments.put(player, this);
		return this;
	}

	protected void removeEnvironment(Player player) {
		environments.remove(player);
	}

	public static IEnvironment get(EntityPlayer player) {
		return get(player.getBukkitEntity());
	}
}
