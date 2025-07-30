package net.dashmc.plots.pipeline.transformers;

import lombok.RequiredArgsConstructor;
import net.dashmc.plots.pipeline.IRenderTransformer;
import net.dashmc.plots.plot.VirtualChunk;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;

@RequiredArgsConstructor
public class BackdropAdderTransformer implements IRenderTransformer {
	private final Chunk backdropChunk;

	@Override
	public void accept(VirtualChunk virtualChunk, ChunkMap chunkMap, Integer metaStartIdx) {
		int mapPointer = 0;

		for (int i = 0; i < 16; i++) {
			ChunkSection section = backdropChunk.getSections()[i];
			if ((chunkMap.b & (1 << i)) == 0)
				continue;
			else if ((section == null)) {
				mapPointer += 4096 * 2;
				continue;
			}

			char[] blockIds = section.getIdArray();
			for (int sectionPointer = 0; sectionPointer < blockIds.length; sectionPointer++, mapPointer += 2) {
				char blockId = (char) ((chunkMap.a[mapPointer] & 0xFF) | ((chunkMap.a[mapPointer + 1] & 0xFF) << 8));
				if (blockId != (char) 0)
					continue;

				char newBlock = blockIds[sectionPointer];
				chunkMap.a[mapPointer] = (byte) (newBlock & 255);
				chunkMap.a[mapPointer + 1] = (byte) ((newBlock >> 8) & 255);
			}
		}
	}

}
