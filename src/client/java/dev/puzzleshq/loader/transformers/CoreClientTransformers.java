package dev.puzzleshq.loader.transformers;

import dev.puzzleshq.loader.launch.PieceClassLoader;
import dev.puzzleshq.loader.mod.entrypoint.TransformerInitializer;

public class CoreClientTransformers implements TransformerInitializer {

    public void onTransformerInit(PieceClassLoader classLoader) {
        classLoader.registerTransformer(new ClientASMTransformer());
    }

}
