package dev.puzzleshq.loader.launch.pieces;

import dev.puzzleshq.loader.launch.Piece;
import dev.puzzleshq.loader.util.EnvType;

public class ClientPiece {

    public static void main(String[] args) {
        Piece.launch(args, EnvType.CLIENT);
    }

}
