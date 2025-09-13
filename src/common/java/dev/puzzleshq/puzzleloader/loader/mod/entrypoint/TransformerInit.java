package dev.puzzleshq.puzzleloader.loader.mod.entrypoint;

import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface TransformerInit {
    String ENTRYPOINT_KEY = "transformers";

    void onTransformerInit(PieceClassLoader classLoader);

    static void invokeTransformers(PieceClassLoader classLoader) {
        PuzzleEntrypointUtil.invoke(ENTRYPOINT_KEY,
                TransformerInit.class,
                transformerInitializer -> {
                    transformerInitializer.onTransformerInit(classLoader);
        });
    }
}
