package net.dashmc.plots.config;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.World;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import net.dashmc.plots.utils.CuboidRegion;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;

@Getter
public class PlotConfig extends OkaeriConfig {

	@Comment("The virtualized region")
	CuboidRegion region;

	public class ChunkConfig extends OkaeriConfig {
		public ChunkCoordIntPair coords;
		public ArrayList<Integer> sections = new ArrayList<>();

		public char getSectionsAsMask() {
			char mask = 0;
			for (int section : sections) {
				mask |= 1 << section;
			}

			return mask;
		}
	}

	@Comment("World to virtualize the chunks in")
	World world = Bukkit.getWorlds().get(0);

}
