package net.dashmc.plots.plot.modifiers;

import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import net.dashmc.plots.plot.PacketModifier;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockDig;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockDig.EnumPlayerDigType;

public class BlockDigPacketModifier extends PacketModifier<PacketPlayInBlockDig> {

	@Override
	public boolean modify(PacketPlayInBlockDig packet, VirtualEnvironment environment) {
		BlockPosition pos = packet.a();
		Chunk chunk = ((CraftWorld) environment.getWorld()).getHandle().getChunkAtWorldCoords(pos);

		VirtualChunk virtualChunk = environment.getVirtualChunks().get(Utils.getCoordHash(chunk.locX, chunk.locZ));
		if (virtualChunk == null)
			return false;

		if (environment.getOwner().getGameMode() == GameMode.CREATIVE
				&& packet.c() == EnumPlayerDigType.START_DESTROY_BLOCK) {
			virtualChunk.setBlock(pos, Blocks.AIR.getBlockData());
		} else if (packet.c() == EnumPlayerDigType.STOP_DESTROY_BLOCK) {
			virtualChunk.setBlock(pos, Blocks.AIR.getBlockData());
		}

		return true;
	}

	@Override
	public Class<PacketPlayInBlockDig> getClazz() {
		return PacketPlayInBlockDig.class;
	}

	public static void register() {
		VirtualEnvironment.register(new BlockDigPacketModifier());
	}
}
