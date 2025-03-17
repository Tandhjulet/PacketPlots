package net.dashmc.plots.config;

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
	HashSet<ChunkCoordIntPair> virtualChunks = new HashSet<>();

	@Comment("World to virtualize the chunks in")
	World world = Bukkit.getWorlds().get(0);

}
