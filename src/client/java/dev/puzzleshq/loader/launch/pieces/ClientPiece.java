package dev.puzzleshq.loader.launch.pieces;

import dev.puzzleshq.loader.launch.PrePiece;
import dev.puzzleshq.loader.util.EnvType;

public class ClientPiece {

    public static void main(String[] args) {
        PrePiece.launch(args, EnvType.CLIENT);
    }

}
