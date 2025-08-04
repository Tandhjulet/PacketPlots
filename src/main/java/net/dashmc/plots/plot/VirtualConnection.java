package net.dashmc.plots.plot;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import lombok.Setter;
import net.dashmc.plots.PacketPlots;
import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.utils.Debug;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListenerPlayIn;
import net.minecraft.server.v1_8_R3.PacketListenerPlayOut;

@Getter
public class VirtualConnection {
	private static final HashMap<Class<?>, PacketInterceptor<?>> packetModifiers = new HashMap<>();

	private @Setter boolean moved = false;

	public static <T extends Packet<?>> void registerInterceptor(PacketInterceptor<T> modifier) {
		packetModifiers.put(modifier.getClazz(), modifier);
	}

	private static final String NETTY_PIPELINE_NAME = "VirtualEnvironment";
	private static HashMap<EntityPlayer, VirtualConnection> connections = new HashMap<>();

	public static VirtualConnection get(Player player) {
		return get(((CraftPlayer) player).getHandle());
	}

	public static VirtualConnection get(EntityPlayer player) {
		return connections.get(player);
	}

	private VirtualConnection(EntityPlayer player, VirtualEnvironment environment) {
		this.player = player;
		this.environment = environment;
		this.original = environment;

		open();
	}

	private EntityPlayer player;
	private VirtualEnvironment environment;
	private VirtualEnvironment original;

	@Setter
	private boolean isVisiting = false;

	public void visit(VirtualEnvironment other) {
		if (other.equals(environment))
			return;

		setVisiting(!other.equals(original));
		environment.getConnections().remove(this);
		other.getConnections().add(this);
		other.render(player);

		Location safeLocation = PacketPlots.getPlotConfig().getSafeLocation();
		Debug.log("teleporting player to " + safeLocation);
		player.getBukkitEntity().teleport(safeLocation);

		this.environment = other;
	}

	public static VirtualConnection establish(EntityPlayer player, VirtualEnvironment environment) {
		return new VirtualConnection(player, environment);
	}

	public void open() {
		Channel channel = player.playerConnection.networkManager.channel;
		if (channel.pipeline().get(NETTY_PIPELINE_NAME) == null) {
			channel.pipeline().addBefore("packet_handler", NETTY_PIPELINE_NAME, new PacketHandler());
		}

		connections.put(player, this);
	}

	public void close() {
		Channel channel = player.playerConnection.networkManager.channel;
		if (channel.pipeline().get(NETTY_PIPELINE_NAME) != null) {
			channel.pipeline().remove(NETTY_PIPELINE_NAME);
		}

		connections.remove(player);
	}

	@SuppressWarnings("unchecked")
	public <T extends Packet<?>> boolean intercept(T packet) {
		PacketInterceptor<T> modifier = (PacketInterceptor<T>) packetModifiers.get(packet.getClass());
		if (modifier != null) {
			return modifier.intercept(packet, this);
		}

		return false;
	}

	/**
	 * PacketHandler for this virtual connection. Sorts through the packets and
	 * passes the important ones to VirtualConnection#intercept.
	 */
	private class PacketHandler extends ChannelDuplexHandler {
		// Outgoing
		@SuppressWarnings("unchecked")
		@Override
		public void write(ChannelHandlerContext chx, Object obj, ChannelPromise promise) throws Exception {
			// Packets to cancel/modify if they occur in the environment

			// Block updates:
			// Block change (https://minecraft.wiki/w/Protocol?oldid=2772100#Block_Change)
			// Multi block change
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Multi_Block_Change)
			// Block action (https://minecraft.wiki/w/Protocol?oldid=2772100#Block_Action)

			Packet<PacketListenerPlayOut> packet = (Packet<PacketListenerPlayOut>) obj;
			if (intercept(packet))
				return;

			super.write(chx, packet, promise);
		}

		// Incomming
		@SuppressWarnings("unchecked")
		@Override
		public void channelRead(ChannelHandlerContext chx, Object obj) throws Exception {
			// Packets to pass to the env. if they occur there

			// Interactions
			// Player Digging
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Digging)
			// Player Block Placement
			// (https://minecraft.wiki/w/Protocol?oldid=2772100#Player_Block_Placement)
			// Update Sign (https://minecraft.wiki/w/Protocol?oldid=2772100#Update_Sign)
			Packet<PacketListenerPlayIn> packet = (Packet<PacketListenerPlayIn>) obj;
			if (intercept(packet))
				return;

			super.channelRead(chx, packet);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.err.println("Netty pipeline exception: " + cause + " (environment owneruuid: "
					+ environment.getOwnerUuid() + ")");
			cause.printStackTrace();
			super.exceptionCaught(ctx, cause);
		}
	}
}
