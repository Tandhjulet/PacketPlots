package net.dashmc.plots.plot.modifiers;

import org.bukkit.Bukkit;

import net.dashmc.plots.plot.PacketModifier;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IContainer;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemBlock;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockPlace;

// https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Digging
public class BlockPlacementPacketModifier extends PacketModifier<PacketPlayInBlockPlace> {

	@Override
	public boolean modify(PacketPlayInBlockPlace packet, VirtualEnvironment environment) {
		BlockPosition pos = packet.a();

		// Is -1 when eating food, pulling back arrows, etc. (listed in wiki)
		if (pos.getY() == -1)
			return false;

		if (packet.getItemStack() == null)
			return handleInteract(packet, environment);

		Item item = packet.getItemStack().getItem();
		if (!(item instanceof ItemBlock))
			return handleInteract(packet, environment);

		BlockPosition offsetPos = offset(pos, packet.getFace());
		VirtualChunk virtualChunk = environment.getVirtualChunks()
				.get(Utils.getCoordHash(offsetPos.getX() >> 4, offsetPos.getZ() >> 4));

		if (virtualChunk == null)
			return false;

		Block block = ((ItemBlock) item).d();

		virtualChunk.setBlock(offsetPos, block.getBlockData());
		return true;
	}

	private boolean handleInteract(PacketPlayInBlockPlace packet, VirtualEnvironment environment) {
		BlockPosition pos = packet.a(); // offset(packet.a(), packet.getFace());
		VirtualChunk virtualChunk = environment.getVirtualChunks()
				.get(Utils.getCoordHash(pos.getX() >> 4, pos.getZ() >> 4));

		// if the interact occured outside the virtual env., let it pass
		if (virtualChunk == null)
			return false;

		IBlockData blockData = virtualChunk.getBlockData(pos);
		if (blockData.getBlock() instanceof IContainer) {
			Bukkit.getLogger().info("clicked on container");
			return true;
		}

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
