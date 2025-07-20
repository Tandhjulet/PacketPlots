package net.dashmc.plots.config.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import net.minecraft.server.v1_8_R3.BlockPosition;

public class BlockPositionSerializer implements ObjectSerializer<BlockPosition> {

	@Override
	public boolean supports(@NonNull Class<? super BlockPosition> type) {
		return BlockPosition.class.isAssignableFrom(type);
	}

	@Override
	public void serialize(@NonNull BlockPosition pos, @NonNull SerializationData data,
			@NonNull GenericsDeclaration generics) {
		data.add("x", pos.getX());
		data.add("y", pos.getY());
		data.add("z", pos.getZ());
	}

	@Override
	public BlockPosition deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
		int x = data.get("x", int.class);
		int y = data.get("y", int.class);
		int z = data.get("z", int.class);
		return new BlockPosition(x, y, z);
	}

}
