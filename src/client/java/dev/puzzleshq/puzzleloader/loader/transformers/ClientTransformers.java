package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInitializer;

public class ClientTransformers implements TransformerInitializer {

    public void onTransformerInit(PieceClassLoader classLoader) {
        classLoader.registerTransformer(new ClientASMTransformer());
    }

}
