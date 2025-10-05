package dev.puzzleshq.puzzleloader.loader.launch.pieces;

public class ServerPiece {

    public static void main(String[] args) {
        System.setProperty("puzzle.core.piece.side", "SERVER");
        NeutralPiece.main(args);
    }

}
