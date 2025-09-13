package net.dashmc.plots.plot;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.bukkit.Bukkit;
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
import net.dashmc.plots.compatibility.CompatibilityMode;
import net.dashmc.plots.compatibility.PluginCompatibility;
import net.dashmc.plots.events.EnvironmentEnterExit;
import net.dashmc.plots.packets.PacketInterceptor;
import net.dashmc.plots.utils.Debug;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketListenerPlayIn;
import net.minecraft.server.v1_8_R3.PacketListenerPlayOut;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;

@Getter
public class VirtualConnection {
	private static Field xCoordField;
	private static Field zCoordField;
	private static Field chunkMapField;
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

	private boolean injected;
	private EntityPlayer player;
	private VirtualEnvironment environment;
	private VirtualEnvironment original;
	private boolean insideEnvironment;

	public void setInsideEnvironment(boolean to) {
		if (to == insideEnvironment)
			return;

		boolean isEntering = to == true && insideEnvironment == false;
		EnvironmentEnterExit event = new EnvironmentEnterExit(this, isEntering);
		Bukkit.getPluginManager().callEvent(event);
	}

	@Setter
	private boolean isVisiting = false;

	public void visit(VirtualEnvironment other) {
		if (other.equals(environment))
			return;

		setVisiting(!other.equals(original));
		environment.getConnections().remove(this);
		other.getConnections().add(this);

		Location safeLocation = PacketPlots.getPlotConfig().getSafeLocation();
		Debug.log("teleporting player to " + safeLocation);
		player.getBukkitEntity().teleport(safeLocation);

		this.environment = other;
		refreshVirtualizedChunks();
	}

	public static PacketPlayOutMapChunk getRenderPacket(int chunkX, int chunkZ, ChunkMap chunkMap) {
		PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk();

		try {
			xCoordField.set(packet, chunkX);
			zCoordField.set(packet, chunkZ);

			if (chunkMap != null)
				chunkMapField.set(packet, chunkMap);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return packet;
	}

	public PacketPlayOutMapChunk getRenderPacket(VirtualChunk vChunk) {
		// as the packet will be intercepted anyways, its a waste to spend time
		// calculating the chunkmap just for it to get thrown away. thus, only calculate
		// if needed
		ChunkMap map;
		if (this.injected && !PluginCompatibility.isActive(CompatibilityMode.FORCE_CHUNKMAP_SEND)) {
			map = null;
		} else {
			map = vChunk.getEnvironment().getRenderPipeline().render(vChunk);
		}

		return getRenderPacket(vChunk.getCoordPair().x, vChunk.getCoordPair().z, map);
	}

	public void refreshVirtualizedChunks() {
		environment.getVirtualChunks().values().forEach((chunk) -> {
			PacketPlayOutMapChunk chunkPacket = getRenderPacket(chunk);
			player.playerConnection.sendPacket(chunkPacket);
		});
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
		this.injected = true;
	}

	public void close() {
		Channel channel = player.playerConnection.networkManager.channel;
		if (channel.pipeline().get(NETTY_PIPELINE_NAME) != null) {
			channel.pipeline().remove(NETTY_PIPELINE_NAME);
		}

		connections.remove(player);
		this.injected = false;
	}

	@SuppressWarnings("unchecked")
	public <T extends Packet<?>> boolean intercept(T packet) {
		PacketInterceptor<T> modifier = (PacketInterceptor<T>) packetModifiers.get(packet.getClass());
		if (modifier != null) {
			return modifier.intercept(packet, this);
		}

		return false;
	}

	static {
		try {
			xCoordField = PacketPlayOutMapChunk.class.getDeclaredField("a");
			xCoordField.setAccessible(true);

			zCoordField = PacketPlayOutMapChunk.class.getDeclaredField("b");
			zCoordField.setAccessible(true);

			chunkMapField = PacketPlayOutMapChunk.class.getDeclaredField("c");
			chunkMapField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * PacketHandler for this virtual connection. Sorts through the packets and
	 * passes the important ones to VirtualConnection#intercept.
	 * 
	 * https://minecraft.wiki/w/Protocol?oldid=2772100
	 */
	private class PacketHandler extends ChannelDuplexHandler {
		// Outgoing
		@SuppressWarnings("unchecked")
		@Override
		public void write(ChannelHandlerContext chx, Object obj, ChannelPromise promise) throws Exception {
			if (!(obj instanceof Packet)) {
				super.write(chx, obj, promise);
				return;
			}

			Packet<PacketListenerPlayOut> packet = (Packet<PacketListenerPlayOut>) obj;
			if (intercept(packet))
				return;

			super.write(chx, packet, promise);
		}

		// Incomming
		@SuppressWarnings("unchecked")
		@Override
		public void channelRead(ChannelHandlerContext chx, Object obj) throws Exception {
			if (!(obj instanceof Packet)) {
				super.channelRead(chx, obj);
				return;
			}

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
