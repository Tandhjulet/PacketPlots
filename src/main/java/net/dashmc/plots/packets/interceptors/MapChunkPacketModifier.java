package net.dashmc.plots.packets.interceptors;

import java.lang.reflect.Field;

import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.plot.VirtualConnection;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;

// https://minecraft.wiki/w/Protocol?oldid=2772100#Chunk_Data
public class MapChunkPacketModifier extends PacketInterceptor<PacketPlayOutMapChunk> {
	private static Field xCoordField;
	private static Field zCoordField;
	private static Field chunkMapField;

	@Override
	public boolean intercept(PacketPlayOutMapChunk packet, VirtualConnection conn) {
		VirtualEnvironment environment = conn.getEnvironment();

		try {
			int xCoord = xCoordField.getInt(packet);
			int zCoord = zCoordField.getInt(packet);

			int hash = Utils.getChunkCoordHash(xCoord, zCoord);
			if (MapChunkBulkPacketModifier.coordPairs.contains(hash)) {
				VirtualChunk chunk = environment.getVirtualChunks().get(hash);
				chunkMapField.set(packet, chunk.getChunkMap(65535, true, true));
			}

		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Class<PacketPlayOutMapChunk> getClazz() {
		return PacketPlayOutMapChunk.class;
	}

	public static void register() {
		VirtualConnection.registerInterceptor(new MapChunkPacketModifier());
	}

	static {
		try {
			xCoordField = PacketPlayOutMapChunk.class.getDeclaredField("a");
			xCoordField.setAccessible(true);
			zCoordField = PacketPlayOutMapChunk.class.getDeclaredField("b");
			zCoordField.setAccessible(true);
			chunkMapField = PacketPlayOutMapChunk.class.getDeclaredField("c");
			chunkMapField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}
}
