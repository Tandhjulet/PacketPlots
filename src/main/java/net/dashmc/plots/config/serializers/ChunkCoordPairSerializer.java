package net.dashmc.plots.config.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;

public class ChunkCoordPairSerializer implements ObjectSerializer<ChunkCoordIntPair> {

	@Override
	public boolean supports(@NonNull Class<? super ChunkCoordIntPair> type) {
		return ChunkCoordIntPair.class.isAssignableFrom(type);
	}

	@Override
	public void serialize(@NonNull ChunkCoordIntPair coordPair, @NonNull SerializationData data,
			@NonNull GenericsDeclaration generics) {
		data.add("x", coordPair.x);
		data.add("z", coordPair.z);
	}

	@Override
	public ChunkCoordIntPair deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
		int x = data.get("x", int.class);
		int z = data.get("z", int.class);
		return new ChunkCoordIntPair(x, z);
	}

}
