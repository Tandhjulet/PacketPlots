package net.dashmc.plots.packets.modifiers;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.util.NumberConversions;

import net.dashmc.plots.packets.PacketModifier;
import net.dashmc.plots.packets.extensions.VirtualBlockChangePacket;
import net.dashmc.plots.plot.VirtualChunk;
import net.dashmc.plots.plot.VirtualEnvironment;
import net.dashmc.plots.utils.Utils;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.ItemBlock;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockPlace;
import net.minecraft.server.v1_8_R3.PacketPlayOutSetSlot;
import net.minecraft.server.v1_8_R3.Slot;

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

		EntityPlayer player = environment.getNMSOwner();
		if (player.dead)
			return true;

		boolean always = false;
		ItemStack inHand = player.inventory.getItemInHand();
		boolean flag = false;
		EnumDirection dir = EnumDirection.fromType1(packet.getFace());

		player.resetIdleTimer();
		if (packet.getFace() == 255) {
			// handle bow stuff and such
		} else if (pos.getY() >= 255 && (dir == EnumDirection.UP || pos.getY() >= 256)) {
			// above build limit
			return true;
		} else {
			Location eyeLoc = player.getBukkitEntity().getEyeLocation();
			double reachDist = NumberConversions.square(eyeLoc.getX() - pos.getX())
					+ NumberConversions.square(eyeLoc.getY() - pos.getY())
					+ NumberConversions.square(eyeLoc.getZ() - pos.getZ());
			if (reachDist > (player.getBukkitEntity().getGameMode() == GameMode.CREATIVE ? 7 * 7 : 6 * 6))
				return true;

			if (player.e((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) < 64D) {
				always = environment.getInteractManager().interact(player, environment, inHand, pos, dir, packet.d(),
						packet.e(), packet.f());
			}

			flag = true;
		}

		if (flag) {
			player.playerConnection.sendPacket(new VirtualBlockChangePacket(environment, pos).getPacket());
			player.playerConnection.sendPacket(new VirtualBlockChangePacket(environment, pos.shift(dir)).getPacket());
		}

		inHand = player.inventory.getItemInHand();
		int inHandIndex = player.inventory.itemInHandIndex;
		if (inHand != null && inHand.count == 0) {
			player.inventory.items[inHandIndex] = null;
			inHand = null;
		}

		if (inHand == null || inHand.l() == 0) {
			player.g = true;
			player.inventory.items[inHandIndex] = ItemStack.b(player.inventory.items[inHandIndex]);
			Slot slot = player.activeContainer.getSlot(player.inventory, inHandIndex);

			player.activeContainer.b();
			player.g = false;

			if (ItemStack.matches(player.inventory.getItemInHand(), packet.getItemStack()) || always) {
				player.playerConnection.sendPacket(new PacketPlayOutSetSlot(player.activeContainer.windowId,
						slot.rawSlotIndex, player.inventory.getItemInHand()));
			}
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
