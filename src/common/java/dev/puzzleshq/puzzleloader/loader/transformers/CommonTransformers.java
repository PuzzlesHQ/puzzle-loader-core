package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInitializer;

public class CommonTransformers implements TransformerInitializer {

    public void onTransformerInit(PieceClassLoader classLoader) {
        classLoader.registerTransformer(new CommonASMTransformer());
    }

}
