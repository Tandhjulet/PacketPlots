package net.dashmc.plots.utils;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.World;

public class Utils {
	public static int getCoordHash(int x, int z) {
		int xHash = 1664525 * x + 1013904223;
		int zHash = 1664525 * (z ^ -559038737) + 1013904223;
		return xHash ^ zHash;
	}

	public static int getChunkCoordHash(BlockPosition pos) {
		return getCoordHash(pos.getX() >> 4, pos.getZ() >> 4);
	}

	public static Location convertPosToLoc(World world, BlockPosition pos) {
		CraftWorld craftWorld = world.getWorld();
		return convertPosToLoc(craftWorld, pos);
	}

	public static Location convertPosToLoc(CraftWorld craftWorld, BlockPosition pos) {
		return convertPosToLoc((org.bukkit.World) craftWorld, pos);
	}

	public static Location convertPosToLoc(org.bukkit.World world, BlockPosition pos) {
		return new Location(world, pos.getX(), pos.getY(), pos.getZ());
	}
}
