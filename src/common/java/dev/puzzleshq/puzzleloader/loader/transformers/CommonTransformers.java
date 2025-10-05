package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInit;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;

public class CommonTransformers implements TransformerInit {

    public void onTransformerInit(PieceClassLoader classLoader) {
        classLoader.registerTransformer("dev.puzzleshq.puzzleloader.loader.transformers.CommonASMTransformer");

        if (Piece.getSide().equals(EnvType.CLIENT)) {
            classLoader.registerTransformers("dev.puzzleshq.puzzleloader.loader.transformers.ClientASMTransformer");
        }
    }

}
