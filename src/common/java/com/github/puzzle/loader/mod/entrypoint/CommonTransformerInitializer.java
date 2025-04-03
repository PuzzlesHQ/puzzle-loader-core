package com.github.puzzle.loader.mod.entrypoint;

import com.github.puzzle.loader.launch.PuzzleClassLoader;
import com.github.puzzle.loader.util.PuzzleEntrypointUtil;

public interface CommonTransformerInitializer {
    String ENTRYPOINT_KEY = "transformers";

    void onTransformerInit(PuzzleClassLoader classLoader);

    static void invokeTransformers(PuzzleClassLoader classLoader) {
        PuzzleEntrypointUtil.invoke(ENTRYPOINT_KEY,
                CommonTransformerInitializer.class,
                transformerInitializer -> {
                    transformerInitializer.onTransformerInit(classLoader);
        });
    }
}
