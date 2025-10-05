package dev.puzzleshq.puzzleloader.loader.launch.pieces;

import dev.puzzleshq.puzzleloader.loader.launch.PrePiece;

public class NeutralPiece {

    public static void main(String[] args) {
        String side = System.getProperty("puzzle.core.piece.side");
        if (side == null) side = "CLIENT";

        PrePiece.launch(args, side);
    }

}
