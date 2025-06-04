package net.dashmc.plots.packets.extensions;

import java.lang.reflect.Field;

import lombok.Getter;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;

public class VirtualBlockChangePacket implements IPacketExtension<PacketPlayOutBlockChange> {
	private static Field posField;

	@Getter
	private final PacketPlayOutBlockChange packet;

	public VirtualBlockChangePacket(VirtualEnvironment env, BlockPosition pos) {
		packet = new PacketPlayOutBlockChange();
		packet.block = env.getType(pos);

		try {
			posField.set(packet, pos);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	static {
		try {
			posField = PacketPlayOutBlockChange.class.getDeclaredField("a");
			posField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}
}