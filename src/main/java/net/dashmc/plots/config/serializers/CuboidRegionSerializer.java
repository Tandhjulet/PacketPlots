package net.dashmc.plots.config.serializers;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;
import net.dashmc.plots.utils.CuboidRegion;
import net.minecraft.server.v1_8_R3.BlockPosition;

public class CuboidRegionSerializer implements ObjectSerializer<CuboidRegion> {

	@Override
	public boolean supports(@NonNull Class<? super CuboidRegion> type) {
		return CuboidRegion.class.isAssignableFrom(type);
	}

	@Override
	public void serialize(@NonNull CuboidRegion region, @NonNull SerializationData data,
			@NonNull GenericsDeclaration generics) {
		data.add("pos1", region.getPos1(), BlockPosition.class);
		data.add("pos2", region.getPos2(), BlockPosition.class);
	}

	@Override
	public CuboidRegion deserialize(@NonNull DeserializationData data, @NonNull GenericsDeclaration generics) {
		BlockPosition pos1 = data.get("pos1", BlockPosition.class);
		BlockPosition pos2 = data.get("pos2", BlockPosition.class);
		return new CuboidRegion(pos1, pos2);
	}
}
