package net.dashmc.plots.packets.interceptors;

import java.lang.reflect.Field;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.util.NumberConversions;

import net.dashmc.plots.events.VirtualPlayerMoveEvent;
import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.plot.VirtualConnection;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying;
import net.minecraft.server.v1_8_R3.PacketPlayOutPosition;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying.PacketPlayInLook;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying.PacketPlayInPosition;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying.PacketPlayInPositionLook;

public class MovementPacketInterceptor extends PacketInterceptor<PacketPlayInFlying> {
	protected static MovementPacketInterceptor INSTANCE;

	@Override
	public boolean intercept(PacketPlayInFlying packet, VirtualConnection connection) {
		double x = packet.a();
		double y = packet.b();
		double z = packet.c();

		if (!NumberConversions.isFinite(x) || !NumberConversions.isFinite(y)
				|| !NumberConversions.isFinite(z) || !NumberConversions.isFinite(packet.d())
				|| !NumberConversions.isFinite(packet.e())) {

			connection.getPlayer().getBukkitEntity().kickPlayer("NaN in position");
			return true;
		}

		if (!connection.getEnvironment().isValidLocation((int) x, (int) y, (int) z))
			return false;
		else if (!connection.isMoved()) {
			connection.setMoved(true);
			return false;
		}

		EntityPlayer player = connection.getPlayer();
		CraftPlayer bukkitPlayer = player.getBukkitEntity();

		Location from = getLocation(connection);
		Location to = bukkitPlayer.getLocation().clone();
		if (packet.g() && !(packet.g() && y == -999D)) {
			to.setX(x);
			to.setY(y);
			to.setZ(z);
		}

		if (packet.h()) {
			to.setYaw(packet.d());
			to.setPitch(packet.e());
		}

		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		double dz = to.getZ() - from.getZ();

		double delta = Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2);
		float deltaAngle = Math.abs(from.getYaw() - to.getYaw()) + Math.abs(from.getPitch() - to.getPitch());

		if ((delta > 1f / 256 || deltaAngle > 10f) && !player.dead) {
			// lastX = to.getX();
			// lastY = to.getY();
			// lastZ = to.getZ();

			Location oldTo = to.clone();
			VirtualPlayerMoveEvent event = new VirtualPlayerMoveEvent(bukkitPlayer, from, oldTo,
					connection.getEnvironment());
			Bukkit.getPluginManager().callEvent(event);

			if (event.isCancelled()) {
				player.playerConnection.sendPacket(new PacketPlayOutPosition(from.getX(), from.getY(), from.getZ(),
						from.getYaw(), from.getPitch(), Collections.emptySet()));
				return true;
			}
		}

		if (player.dead)
			return true;

		if (player.isSleeping()) {
			// player.l();
			player.setLocation(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
			return true;
		}

		// set last loc
		setLocation(to, connection);

		if (packet.g() && packet.b() == -999D)
			packet.a(false);

		if (packet.g()) {
			if (Math.abs(packet.a()) > 3.0E7D || Math.abs(packet.c()) > 3.0E7D) {
				player.playerConnection.disconnect("Illegal position");
				return true;
			}
		}

		// player.l();
		player.setLocation(to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());

		return true;
	}

	private static Field xCoord;
	private static Field yCoord;
	private static Field zCoord;

	private static Field yaw;
	private static Field pitch;

	private static Field hasMoved;
	private static Field h;

	static {
		try {
			hasMoved = PlayerConnection.class.getDeclaredField("hasMoved");
			hasMoved.setAccessible(true);

			h = PlayerConnection.class.getDeclaredField("h");
			h.setAccessible(true);

			xCoord = PlayerConnection.class.getDeclaredField("o");
			yCoord = PlayerConnection.class.getDeclaredField("p");
			zCoord = PlayerConnection.class.getDeclaredField("q");

			xCoord.setAccessible(true);
			yCoord.setAccessible(true);
			zCoord.setAccessible(true);

			yaw = PlayerConnection.class.getDeclaredField("lastYaw");
			pitch = PlayerConnection.class.getDeclaredField("lastPitch");

			yaw.setAccessible(true);
			pitch.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}

	private static void setLocation(Location loc, VirtualConnection vConn) {
		PlayerConnection conn = vConn.getPlayer().playerConnection;

		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();

		try {
			xCoord.setDouble(conn, x);
			yCoord.setDouble(conn, y);
			zCoord.setDouble(conn, z);

			yaw.setFloat(conn, loc.getYaw());
			pitch.setFloat(conn, loc.getPitch());
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static Location getLocation(VirtualConnection vConn) {
		PlayerConnection conn = vConn.getPlayer().playerConnection;

		try {
			double y = (double) yCoord.get(conn);
			double z = (double) zCoord.get(conn);
			double x = (double) xCoord.get(conn);

			float cYaw = (float) yaw.get(conn);
			float cPitch = (float) pitch.get(conn);

			return new Location(vConn.getEnvironment().getWorld(), x, y, z, cYaw, cPitch);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Class<PacketPlayInFlying> getClazz() {
		return PacketPlayInFlying.class;
	}

	public static void register() {
		INSTANCE = new MovementPacketInterceptor();
		VirtualConnection.registerInterceptor(INSTANCE);
		VirtualConnection.registerInterceptor(new LookPacketInterceptor());
		VirtualConnection.registerInterceptor(new PositionPacketInterceptor());
		VirtualConnection.registerInterceptor(new PositionLookPacketInterceptor());
	}

	public static class LookPacketInterceptor extends PacketInterceptor<PacketPlayInLook> {
		@Override
		public boolean intercept(PacketPlayInLook packet, VirtualConnection connection) {
			return INSTANCE.intercept(packet, connection);
		}

		@Override
		public Class<PacketPlayInLook> getClazz() {
			return PacketPlayInLook.class;
		}
	}

	public static class PositionPacketInterceptor extends PacketInterceptor<PacketPlayInPosition> {
		@Override
		public boolean intercept(PacketPlayInPosition packet, VirtualConnection connection) {
			return INSTANCE.intercept(packet, connection);
		}

		@Override
		public Class<PacketPlayInPosition> getClazz() {
			return PacketPlayInPosition.class;
		}
	}

	public static class PositionLookPacketInterceptor extends PacketInterceptor<PacketPlayInPositionLook> {
		@Override
		public boolean intercept(PacketPlayInPositionLook packet, VirtualConnection connection) {
			return INSTANCE.intercept(packet, connection);
		}

		@Override
		public Class<PacketPlayInPositionLook> getClazz() {
			return PacketPlayInPositionLook.class;
		}
	}

}
