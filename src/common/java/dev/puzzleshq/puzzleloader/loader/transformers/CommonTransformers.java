package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInit;

public class CommonTransformers implements TransformerInit {

    public void onTransformerInit(PieceClassLoader classLoader) {
        classLoader.registerTransformer("dev.puzzleshq.puzzleloader.loader.transformers.CommonASMTransformer");
    }

}
