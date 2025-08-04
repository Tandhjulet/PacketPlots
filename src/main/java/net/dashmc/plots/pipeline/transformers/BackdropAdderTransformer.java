package net.dashmc.plots.pipeline.transformers;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.dashmc.plots.pipeline.IRenderTransformer;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.utils.CuboidRegion;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;

@RequiredArgsConstructor
@AllArgsConstructor
public class BackdropAdderTransformer implements IRenderTransformer {
	private final Chunk backdropChunk;
	private boolean includeVirtualizedRegion = false;

	@Override
	public void accept(VirtualChunk virtualChunk, ChunkMap chunkMap, Integer metaStartIdx) {
		int mapPointer = 0;
		CuboidRegion region = virtualChunk.getEnvironment().getRegion();

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

				if (!includeVirtualizedRegion) {
					int shortIdx = mapPointer >> 1;
					int x = (shortIdx & 0xF) + (virtualChunk.getCoordPair().x << 4);
					int y = (shortIdx >> 8) & 0xFF;
					int z = ((shortIdx >> 4) & 0xF) + (virtualChunk.getCoordPair().z << 4);

					if (region.includes(x, y, z))
						continue;
				}

				char newBlock = blockIds[sectionPointer];
				chunkMap.a[mapPointer] = (byte) (newBlock & 255);
				chunkMap.a[mapPointer + 1] = (byte) ((newBlock >> 8) & 255);
			}
		}
	}

}
