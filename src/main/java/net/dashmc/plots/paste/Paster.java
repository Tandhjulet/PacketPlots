package net.dashmc.plots.paste;

import java.lang.reflect.InvocationTargetException;

import net.dashmc.plots.pipeline.RenderPipeline;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.utils.MethodWrapper;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;
import net.minecraft.server.v1_8_R3.WorldServer;

public class Paster {
	public static boolean paste(VirtualChunk chunk, RenderPipeline pipeline, ChunkCoordIntPair at) {
		int atChunkX = at.x, atChunkZ = at.z;

		MethodWrapper<Void> packetSender = Utils.getRelatedPlayerPacketSender(atChunkX, atChunkZ,
				(WorldServer) chunk.getChunk().getWorld());
		if (packetSender == null)
			return false;

		ChunkMap map = pipeline.render(chunk);

		try {
			PacketPlayOutMapChunk packet = VirtualChunk.getRenderPacket(atChunkX, atChunkZ, map);
			packetSender.call(packet);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		Chunk nmsChunk = chunk.getChunk().getWorld().getChunkAt(atChunkX, atChunkZ);

		nmsChunk.f(true);
		nmsChunk.mustSave = true;

		int nonEmptyChunkSections = Integer.bitCount(map.b);
		int metaStartIdx = nonEmptyChunkSections * 2 * 16 * 16 * 16 + nonEmptyChunkSections * 16 * 16 * 8;

		int idPointer = 0;

		char[] blockIds = new char[4096];
		for (int i = 0; i < metaStartIdx; i += 2) {
			char blockId = (char) ((map.a[i] & 0xFF) | ((map.a[i + 1] & 0xFF) << 8));
			blockIds[idPointer++] = blockId;

			if (idPointer == 4096) {
				idPointer = 0;
				int chunkY = (((i >> 1) >> 8) & 0xFF) >> 4;

				ChunkSection section = new ChunkSection(chunkY << 4, true, blockIds);
				nmsChunk.getSections()[chunkY] = section;

				blockIds = new char[4096];
			}
		}

		nmsChunk.initLighting();

		return true;

	}
}
