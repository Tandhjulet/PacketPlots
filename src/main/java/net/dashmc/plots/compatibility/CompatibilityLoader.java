package net.dashmc.plots.compatibility;

public abstract class CompatibilityLoader {
	public final CompatibilityLoader INSTANCE;

	public CompatibilityLoader() {
		this.INSTANCE = this;
	}

	public abstract boolean shouldActivate();

	public void activate() {
		activate(false);
	}

	public abstract void activate(boolean forced);
}
