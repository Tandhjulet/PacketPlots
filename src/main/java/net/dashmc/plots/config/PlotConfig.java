package net.dashmc.plots.config;

import java.util.HashSet;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;

@Getter
public class PlotConfig extends OkaeriConfig {

	@Comment("Chunk coord pairs for the chunks to be virtualized")
	public HashSet<ChunkCoordIntPair> virtualChunks = new HashSet<>();

}
