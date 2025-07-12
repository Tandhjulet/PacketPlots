package net.dashmc.plots.packets.interceptors;

import org.bukkit.Bukkit;

import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.plot.VirtualConnection;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;

public class UseEntityPacketInterceptor extends PacketInterceptor<PacketPlayInUseEntity> {

	@Override
	public boolean intercept(PacketPlayInUseEntity packet, VirtualConnection environment) {
		Bukkit.getLogger().info("use entity");
		return false;
	}

	@Override
	public Class<PacketPlayInUseEntity> getClazz() {
		return PacketPlayInUseEntity.class;
	}

	public static void register() {
		VirtualConnection.registerInterceptor(new UseEntityPacketInterceptor());
	}

}
