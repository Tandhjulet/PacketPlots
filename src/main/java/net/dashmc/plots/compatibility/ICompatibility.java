package net.dashmc.plots.compatibility;

public interface ICompatibility {
	public boolean shouldActivate();

	default public void activate() {
		activate(false);
	}

	public void activate(boolean forced);
}
