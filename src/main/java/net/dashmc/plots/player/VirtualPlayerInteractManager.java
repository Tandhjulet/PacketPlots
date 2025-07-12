package net.dashmc.plots.player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Material;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;

public class VirtualPlayerInteractManager extends PlayerInteractManager {
	private static Field playerInteractManagerField;
	private EntityPlayer player;

	public static VirtualPlayerInteractManager inject(Player player)
			throws IllegalArgumentException, IllegalAccessException {
		return inject(((CraftPlayer) player).getHandle());
	}

	public static VirtualPlayerInteractManager inject(EntityPlayer player)
			throws IllegalArgumentException, IllegalAccessException {
		VirtualPlayerInteractManager manager = new VirtualPlayerInteractManager(player);
		playerInteractManagerField.set(player, manager);
		return manager;
	}

	private void copyFields(PlayerInteractManager source) {
		Class<?> clazz = source.getClass();
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				try {
					field.set(this, field.get(source));
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Failed to copy field: " + field.getName(), e);
				}
			}
			clazz = clazz.getSuperclass();
		}
	}

	private VirtualPlayerInteractManager(EntityPlayer player) {
		super(player.getWorld());
		copyFields(player.playerInteractManager);
		this.player = player;
	}

	// called every tick
	@Override
	public void a() {
		if (!isDestroying()) {
			super.a();
			return;
		}

		BlockPosition pos = getDestroyPosition();
		VirtualEnvironment env = VirtualEnvironment.get(player);
		if (env == null || !env.isValidLocation(pos)) {
			super.a();
			return;
		}

		setCurrentTick(MinecraftServer.currentTick);

		Block block = env.getType(pos).getBlock();
		if (block.getMaterial() == Material.AIR) {
			env.broadcastPacket(new PacketPlayOutBlockBreakAnimation(player.getId(), pos, -1));
			setForce(0);
			setIsDestroying(false);
			return;
		}

		int k = getCurrentTick() - getLastDigTick();
		float f = block.getDamage(player, world, pos) * (float) (k + 1);
		int force = (int) (f * 10.0F);
		if (force != getForce()) {
			env.broadcastPacket(new PacketPlayOutBlockBreakAnimation(player.getId(), pos, force));
			setForce(force);
		}
	}

	static {
		try {
			playerInteractManagerField = EntityPlayer.class.getDeclaredField("playerInteractManager");
			playerInteractManagerField.setAccessible(true);

			// as the field is declared as final, we need to remove the final modifier
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(playerInteractManagerField, playerInteractManagerField.getModifiers() & ~Modifier.FINAL);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	// player interact manager utils
	private static Field lastDigTick;
	private static Field currentTick;
	private static Field isDestroying;
	private static Field destroyPosition;
	private static Field force;

	public int getLastDigTick() {
		try {
			return (int) lastDigTick.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public void setLastDigTick(int val) {
		try {
			lastDigTick.set(this, val);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public void setCurrentTick(int val) {
		try {
			currentTick.set(this, val);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public int getCurrentTick() {
		try {
			return (int) currentTick.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public BlockPosition getDestroyPosition() {
		try {
			return (BlockPosition) destroyPosition.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setDestroyPosition(BlockPosition to) {
		try {
			destroyPosition.set(this, to);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public void setIsDestroying(boolean to) {
		try {
			isDestroying.set(this, to);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public boolean isDestroying() {
		try {
			return (boolean) isDestroying.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void setForce(int to) {
		try {
			force.set(this, to);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public int getForce() {
		try {
			return (int) force.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return 0;
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

}
