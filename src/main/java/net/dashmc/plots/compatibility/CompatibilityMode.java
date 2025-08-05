package net.dashmc.plots.compatibility;

import lombok.Getter;
import net.dashmc.plots.compatibility.compatibilities.ChunkMapSendCompatibility;

public enum CompatibilityMode {
	VULCAN(),
	FORCE_CHUNKMAP_SEND(new ChunkMapSendCompatibility());

	@Getter
	private final ICompatibility[] loaders;

	CompatibilityMode(ICompatibility... loaders) {
		this.loaders = loaders;
	}

	public boolean shouldActivate() {
		for (ICompatibility loader : loaders) {
			if (loader.shouldActivate())
				return true;
		}

		return false;
	}

	public void activate() {
		activate(false);
	}

	public void activate(boolean forced) {
		for (ICompatibility loader : loaders) {
			if (forced || loader.shouldActivate())
				loader.activate(forced);
		}
	}
}
