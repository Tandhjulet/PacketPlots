package net.dashmc.plots.config;

import java.util.ArrayList;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.World;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;

@Getter
public class PlotConfig extends OkaeriConfig {

	@Comment("Chunk coord pairs for the chunks to be virtualized")
	HashSet<ChunkConfig> virtualChunks = new HashSet<>();

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
