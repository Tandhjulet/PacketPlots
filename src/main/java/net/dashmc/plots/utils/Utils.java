package net.dashmc.plots.utils;

import net.minecraft.server.v1_8_R3.BlockPosition;

public class Utils {
	public static int getCoordHash(int x, int z) {
		int xHash = 1664525 * x + 1013904223;
		int zHash = 1664525 * (z ^ -559038737) + 1013904223;
		return xHash ^ zHash;
	}

	public static int getCoordHash(BlockPosition pos) {
		return getCoordHash(pos.getX() >> 4, pos.getZ() >> 4);
	}
}
