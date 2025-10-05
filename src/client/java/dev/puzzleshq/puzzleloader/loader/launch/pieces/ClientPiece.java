package dev.puzzleshq.puzzleloader.loader.launch.pieces;

public class ClientPiece {

    public static void main(String[] args) {
        System.setProperty("puzzle.core.piece.side", "CLIENT");
        NeutralPiece.main(args);
    }

}
