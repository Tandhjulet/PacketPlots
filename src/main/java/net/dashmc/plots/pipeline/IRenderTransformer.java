package net.dashmc.plots.pipeline;

import org.apache.logging.log4j.util.TriConsumer;

import net.dashmc.plots.plot.VirtualChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;

public interface IRenderTransformer extends TriConsumer<VirtualChunk, ChunkMap, Integer> {
	@Override
	void accept(VirtualChunk virtualChunk, ChunkMap chunkMap, Integer metaStartIdx);
}
