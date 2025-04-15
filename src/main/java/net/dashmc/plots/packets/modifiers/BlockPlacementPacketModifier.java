package net.dashmc.plots.packets.modifiers;

import net.dashmc.plots.packets.PacketModifier;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ItemBlock;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockPlace;

// https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Digging
// https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/PlayerConnection.java#L638
public class BlockPlacementPacketModifier extends PacketModifier<PacketPlayInBlockPlace> {

	@Override
	public boolean modify(PacketPlayInBlockPlace packet, VirtualEnvironment environment) {
		BlockPosition pos = packet.a();
		if (packet.getItemStack() != null && packet.getItemStack().getItem() instanceof ItemBlock)
			pos = offset(pos, packet.getFace());

		VirtualChunk virtualChunk = environment.getVirtualChunks()
				.get(Utils.getCoordHash(pos.getX() >> 4, pos.getZ() >> 4));
		if (virtualChunk == null)
			return false;

		environment.getVirtualConnection().onBlockPlace(packet);
		return true;
	}

	private BlockPosition offset(BlockPosition toOffset, int face) {
		if (face == 0)
			return toOffset.down();
		else if (face == 1)
			return toOffset.up();
		else if (face == 2)
			return toOffset.north();
		else if (face == 3)
			return toOffset.south();
		else if (face == 4)
			return toOffset.west();
		else if (face == 5)
			toOffset.east();

		return toOffset;
	}

	@Override
	public Class<PacketPlayInBlockPlace> getClazz() {
		return PacketPlayInBlockPlace.class;
	}

	public static void register() {
		VirtualEnvironment.register(new BlockPlacementPacketModifier());
	}
}
