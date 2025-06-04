package net.dashmc.plots.packets.extensions;

import net.minecraft.server.v1_8_R3.Packet;

public interface IPacketExtension<T extends Packet<?>> {
	public T getPacket();
}
