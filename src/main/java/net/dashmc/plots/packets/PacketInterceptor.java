package net.dashmc.plots.packets;

import net.dashmc.plots.packets.interceptors.BlockDigPacketModifier;
import net.dashmc.plots.packets.interceptors.BlockPlacementPacketModifier;
import net.dashmc.plots.packets.interceptors.MapChunkBulkPacketModifier;
import net.dashmc.plots.packets.interceptors.MapChunkPacketModifier;
import net.dashmc.plots.packets.interceptors.MovementPacketInterceptor;
import net.dashmc.plots.packets.interceptors.UseEntityPacketModifier;
import net.dashmc.plots.packets.interceptors.WindowClosePacketModifier;
import net.dashmc.plots.plot.VirtualConnection;
import net.minecraft.server.v1_8_R3.Packet;

public abstract class PacketInterceptor<T extends Packet<?>> {
	public PacketInterceptor() {
	}

	public abstract boolean intercept(T packet, VirtualConnection connection);

	public abstract Class<T> getClazz();

	public static void register() {
		MapChunkBulkPacketModifier.register();
		MapChunkPacketModifier.register();
		BlockDigPacketModifier.register();
		BlockPlacementPacketModifier.register();
		UseEntityPacketModifier.register();
		WindowClosePacketModifier.register();
		MovementPacketInterceptor.register();
	}
}