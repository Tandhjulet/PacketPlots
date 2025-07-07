package net.dashmc.plots.utils;

import java.lang.reflect.Field;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.World;

public class Utils {
	// player interact manager utils
	private static Field lastDigTick;
	private static Field currentTick;
	private static Field isDestroying;
	private static Field destroyPosition;
	private static Field force;

	public static int getLastDigTick(PlayerInteractManager manager) {
		try {
			return (int) lastDigTick.get(manager);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public static void setLastDigTick(PlayerInteractManager manager, int val) {
		try {
			lastDigTick.set(manager, val);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void setCurrentTick(PlayerInteractManager manager, int val) {
		try {
			currentTick.set(manager, val);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static int getCurrentTick(PlayerInteractManager manager) {
		try {
			return (int) currentTick.get(manager);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public static BlockPosition getDestroyPosition(PlayerInteractManager manager) {
		try {
			return (BlockPosition) destroyPosition.get(manager);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void setDestroyPosition(PlayerInteractManager manager, BlockPosition to) {
		try {
			destroyPosition.set(manager, to);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void setIsDestroying(PlayerInteractManager manager, boolean to) {
		try {
			isDestroying.set(manager, to);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void setForce(PlayerInteractManager manager, int to) {
		try {
			force.set(manager, to);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	static {
		try {
			lastDigTick = PlayerInteractManager.class.getDeclaredField("lastDigTick");
			lastDigTick.setAccessible(true);

			currentTick = PlayerInteractManager.class.getDeclaredField("currentTick");
			currentTick.setAccessible(true);

			isDestroying = PlayerInteractManager.class.getDeclaredField("d");
			isDestroying.setAccessible(true);

			destroyPosition = PlayerInteractManager.class.getDeclaredField("f");
			destroyPosition.setAccessible(true);

			force = PlayerInteractManager.class.getDeclaredField("k");
			force.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}

	}

	// chunk utils
	public static int getChunkCoordHash(int x, int z) {
		int xHash = 1664525 * x + 1013904223;
		int zHash = 1664525 * (z ^ -559038737) + 1013904223;
		return xHash ^ zHash;
	}

	public static int getChunkCoordHash(BlockPosition pos) {
		return getChunkCoordHash(pos.getX() >> 4, pos.getZ() >> 4);
	}

	// coord utils
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

	public static BlockPosition convertLocToPos(Location loc) {
		return new BlockPosition(loc.getX(), loc.getY(), loc.getZ());
	}
}
