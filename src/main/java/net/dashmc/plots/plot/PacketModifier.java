package net.dashmc.plots.plot;

import net.dashmc.plots.plot.modifiers.BlockDigPacketModifier;
import net.dashmc.plots.plot.modifiers.BlockPlacementPacketModifier;
import net.dashmc.plots.plot.modifiers.MapChunkBulkPacketModifier;
import net.dashmc.plots.plot.modifiers.MapChunkPacketModifier;
import net.dashmc.plots.plot.modifiers.UseEntityPacketModifier;
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
	}
}