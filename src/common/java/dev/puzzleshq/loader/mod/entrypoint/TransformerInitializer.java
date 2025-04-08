package dev.puzzleshq.loader.mod.entrypoint;

import dev.puzzleshq.loader.launch.PieceClassLoader;
import dev.puzzleshq.loader.util.PuzzleEntrypointUtil;

public interface TransformerInitializer {
    String ENTRYPOINT_KEY = "transformers";

    void onTransformerInit(PieceClassLoader classLoader);

    static void invokeTransformers(PieceClassLoader classLoader) {
        PuzzleEntrypointUtil.invoke(ENTRYPOINT_KEY,
                TransformerInitializer.class,
                transformerInitializer -> {
                    transformerInitializer.onTransformerInit(classLoader);
        });
    }
}
