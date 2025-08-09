package net.dashmc.plots.packets.interceptors;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.event.CraftEventFactory;

import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.plot.VirtualConnection;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.Debug;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockChest;
import net.minecraft.server.v1_8_R3.BlockEnderChest;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Container;
import net.minecraft.server.v1_8_R3.ContainerChest;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IInventory;
import net.minecraft.server.v1_8_R3.InventoryEnderChest;
import net.minecraft.server.v1_8_R3.InventoryLargeChest;
import net.minecraft.server.v1_8_R3.PacketPlayInCloseWindow;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockAction;
import net.minecraft.server.v1_8_R3.PlayerInventory;
import net.minecraft.server.v1_8_R3.TileEntityChest;

public class WindowClosePacketInterceptor extends PacketInterceptor<PacketPlayInCloseWindow> {
	private static Method closeContainerMethod;

	@Override
	public boolean intercept(PacketPlayInCloseWindow packet, VirtualConnection connection) {
		Debug.log("window close packet!");

		VirtualEnvironment environment = connection.getEnvironment();
		EntityPlayer player = connection.getPlayer();
		PlayerInventory playerInventory = player.inventory;

		if (!(player.activeContainer instanceof ContainerChest))
			return false;

		ContainerChest chest = (ContainerChest) player.activeContainer;
		if (isCustomInventory(chest.e()))
			return false;

		Debug.log("packet will be intercepted.");

		CraftEventFactory.handleInventoryCloseEvent(player);

		boolean success = false;
		if (chest.e() instanceof TileEntityChest) { // single chest
			if (!isChestLocationValid(chest.e(), environment))
				return false;

			success = closeContainer(player, chest.e(), environment);
		} else if (chest.e() instanceof InventoryLargeChest) { // chest consisting of two chests
			InventoryLargeChest doubleChest = (InventoryLargeChest) chest.e();

			boolean isLeftValid = isChestLocationValid(doubleChest.left, environment);
			boolean isRightValid = isChestLocationValid(doubleChest.right, environment);
			if (isLeftValid ^ isRightValid) {
				throw new RuntimeException("Location of double chest is half outside of virtual environment ("
						+ connection.getEnvironment().getOwnerUuid() + ")");
			} else if (!isLeftValid && !isRightValid)
				return false;

			success = closeContainer(player, doubleChest.left, environment)
					&& closeContainer(player, doubleChest.right, environment);
		} else if (chest.e() instanceof InventoryEnderChest) {
			InventoryEnderChest enderChest = (InventoryEnderChest) chest.e();
			enderChest.closeContainer(player);

			success = true;

		} else {
			Bukkit.getLogger().warning("Window close packet sent with unrecognized inventory: " + chest.e().getClass());
		}

		if (success) {
			if (playerInventory.getCarried() != null) {
				player.drop(playerInventory.getCarried(), false);
				playerInventory.setCarried(null);
			}

			player.activeContainer = player.defaultContainer;
			return true;
		}
		// intercept packet to prevent dupes - do nothing tho
		return true;
	}

	private boolean isCustomInventory(IInventory inventory) {
		Class<?> enclosing = inventory.getClass().getEnclosingClass();
		return enclosing != null && enclosing.getName().endsWith(".CraftInventoryCustom");
	}

	public boolean isChestLocationValid(IInventory inventory, VirtualEnvironment env) {
		if (!(inventory instanceof TileEntityChest))
			return false;

		TileEntityChest chest = (TileEntityChest) inventory;
		BlockPosition pos = chest.getPosition();
		if (!env.isValidLocation(pos))
			return false;
		return env.getType(pos).getBlock() instanceof BlockChest;
	}

	public boolean closeContainer(EntityHuman human, IInventory inventory, VirtualEnvironment env) {
		if (!(inventory instanceof TileEntityChest))
			return false;

		TileEntityChest chest = (TileEntityChest) inventory;
		BlockPosition pos = chest.getPosition();
		if (!env.isValidLocation(pos))
			return false;
		else if (human.isSpectator())
			return true;

		// let them close even though it wasnt a success
		if (!(env.getType(pos).getBlock() instanceof BlockChest))
			return true;

		chest.l--;

		Block block = env.getType(chest.getPosition()).getBlock();
		PacketPlayOutBlockAction chestClosePacket = new PacketPlayOutBlockAction(chest.getPosition(), block, 1, 0);
		env.broadcastPacket(chestClosePacket);

		return true;
	}

	@Override
	public Class<PacketPlayInCloseWindow> getClazz() {
		return PacketPlayInCloseWindow.class;
	}

	public static void register() {
		VirtualConnection.registerInterceptor(new WindowClosePacketInterceptor());
	}

	static {
		try {
			closeContainerMethod = Container.class.getDeclaredMethod("b", EntityHuman.class);
			closeContainerMethod.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

}
