package net.dashmc.plots.packets;

import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.packets.modifiers.BlockDigPacketModifier;
import net.dashmc.plots.packets.modifiers.BlockPlacementPacketModifier;
import net.dashmc.plots.packets.modifiers.MapChunkBulkPacketModifier;
import net.dashmc.plots.packets.modifiers.MapChunkPacketModifier;
import net.dashmc.plots.packets.modifiers.UseEntityPacketModifier;
import net.dashmc.plots.packets.modifiers.WindowClosePacketModifier;
import net.minecraft.server.v1_8_R3.Packet;

public abstract class PacketModifier<T extends Packet<?>> {
	public PacketModifier() {
	}

	public abstract boolean modify(T packet, VirtualEnvironment environment);

	public abstract Class<T> getClazz();

	public static void register() {
		MapChunkBulkPacketModifier.register();
		MapChunkPacketModifier.register();
		BlockDigPacketModifier.register();
		BlockPlacementPacketModifier.register();
		UseEntityPacketModifier.register();
		WindowClosePacketModifier.register();
	}
}