package net.dashmc.plots.packets.modifiers;

import org.bukkit.Bukkit;

import net.dashmc.plots.packets.PacketModifier;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;

public class UseEntityPacketModifier extends PacketModifier<PacketPlayInUseEntity> {

	@Override
	public boolean modify(PacketPlayInUseEntity packet, VirtualEnvironment environment) {
		Bukkit.getLogger().info("use entity");
		return false;
	}

	@Override
	public Class<PacketPlayInUseEntity> getClazz() {
		return PacketPlayInUseEntity.class;
	}

	public static void register() {
		VirtualEnvironment.register(new UseEntityPacketModifier());
	}

}
