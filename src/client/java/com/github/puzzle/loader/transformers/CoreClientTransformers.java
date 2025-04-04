package com.github.puzzle.loader.transformers;

import com.github.puzzle.loader.launch.PuzzleClassLoader;
import com.github.puzzle.loader.mod.entrypoint.CommonTransformerInitializer;

public class CoreClientTransformers implements CommonTransformerInitializer {

    public void onTransformerInit(PuzzleClassLoader classLoader) {
        classLoader.registerTransformer("com.github.puzzle.loader.transformers.ClientASMTransformer");
    }

}
