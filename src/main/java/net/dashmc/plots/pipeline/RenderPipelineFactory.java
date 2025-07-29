package net.dashmc.plots.pipeline;

import java.util.LinkedList;

public class RenderPipelineFactory {
	private LinkedList<IRenderTransformer> transformers = new LinkedList<>();

	public RenderPipelineFactory addTransformer(IRenderTransformer transformer) {
		transformers.add(transformer);
		return this;
	}

	public RenderPipeline build() {
		return new RenderPipeline(transformers);
	}

	public static RenderPipeline createEmptyPipeline() {
		return new RenderPipelineFactory().build();
	}
}
