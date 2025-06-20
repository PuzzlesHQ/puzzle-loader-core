package dev.puzzleshq.puzzleloader.loader.launch.pieces;

import dev.puzzleshq.puzzleloader.loader.launch.PrePiece;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;

public class ClientPiece {

    public static void main(String[] args) {
        PrePiece.launch(args, EnvType.CLIENT);
    }

}
