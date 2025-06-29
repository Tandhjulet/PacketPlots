package net.dashmc.plots.packets.interceptors;

import java.lang.reflect.Method;

import org.bukkit.craftbukkit.v1_8_R3.event.CraftEventFactory;

import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.plot.VirtualConnection;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockChest;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Container;
import net.minecraft.server.v1_8_R3.ContainerChest;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayInCloseWindow;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockAction;
import net.minecraft.server.v1_8_R3.PlayerInventory;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.TileEntityChest;

public class WindowClosePacketModifier extends PacketInterceptor<PacketPlayInCloseWindow> {
	private static Method closeContainerMethod;

	@Override
	public boolean intercept(PacketPlayInCloseWindow packet, VirtualConnection connection) {
		VirtualEnvironment environment = connection.getEnvironment();
		EntityPlayer player = connection.getPlayer();
		CraftEventFactory.handleInventoryCloseEvent(player);

		if (!(player.activeContainer instanceof ContainerChest))
			return false;

		ContainerChest chest = (ContainerChest) player.activeContainer;
		TileEntity tile = (TileEntity) chest.e();
		BlockPosition pos = tile.getPosition();

		if (!environment.isValidLocation(pos))
			return false;

		Block block = environment.getType(pos).getBlock();

		if (!(block instanceof BlockChest) || !(tile instanceof TileEntityChest))
			throw new RuntimeException("Block isn't a chest... if this ever happens, dupes might be possible.");

		TileEntityChest tileEntityChest = (TileEntityChest) tile;

		PlayerInventory playerInventory = player.inventory;
		if (playerInventory.getCarried() != null) {
			player.drop(playerInventory.getCarried(), false);
			playerInventory.setCarried(null);
		}
		tileEntityChest.l--;

		PacketPlayOutBlockAction chestOpenPacket = new PacketPlayOutBlockAction(pos, block, 1, 0);
		player.playerConnection.sendPacket(chestOpenPacket);

		player.activeContainer = player.defaultContainer;
		return true;
	}

	@Override
	public Class<PacketPlayInCloseWindow> getClazz() {
		return PacketPlayInCloseWindow.class;
	}

	public static void register() {
		VirtualConnection.registerInterceptor(new WindowClosePacketModifier());
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
