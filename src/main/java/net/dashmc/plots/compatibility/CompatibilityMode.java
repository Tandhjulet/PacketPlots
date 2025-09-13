package net.dashmc.plots.compatibility;

import lombok.Getter;
import net.dashmc.plots.compatibility.compatibilities.ChunkMapSendCompatibility;
import net.dashmc.plots.compatibility.compatibilities.HDBCompatibility;
import net.dashmc.plots.compatibility.compatibilities.NCPCompatibility;
import net.dashmc.plots.compatibility.compatibilities.VulcanCompatibility;

public enum CompatibilityMode {
	VULCAN(new VulcanCompatibility()),
	FORCE_CHUNKMAP_SEND(new ChunkMapSendCompatibility()),
	HEAD_DATABASE(new HDBCompatibility()),
	NO_CHEAT_PLUS(new NCPCompatibility());

	@Getter
	private final CompatibilityLoader[] loaders;

	CompatibilityMode(CompatibilityLoader... loaders) {
		this.loaders = loaders;
	}

	public boolean shouldActivate() {
		for (CompatibilityLoader loader : loaders) {
			if (loader.shouldActivate())
				return true;
		}

		return false;
	}

	public void activate() {
		activate(false);
	}

	public void activate(boolean forced) {
		for (CompatibilityLoader loader : loaders) {
			if (forced || loader.shouldActivate())
				loader.activate(forced);
		}
	}
}
