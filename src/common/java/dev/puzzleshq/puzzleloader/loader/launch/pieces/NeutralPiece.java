package dev.puzzleshq.puzzleloader.loader.launch.pieces;

import dev.puzzleshq.puzzleloader.loader.launch.PrePiece;

/**
 * Entry point for launching a neutral puzzle piece.
 * <p>
 * Determines the side of the puzzle piece (client or server) via the system property
 * {@code puzzle.core.piece.side} and delegates the launch process to {@link PrePiece}.
 * Defaults to {@code CLIENT} if the system property is not set.
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * java -Dpuzzle.core.piece.side=SERVER dev.puzzleshq.puzzleloader.loader.launch.pieces.NeutralPiece
 * </pre>
 * </p>
 */
public class NeutralPiece {

    /**
     * Main entry point for the neutral puzzle piece launcher.
     * <p>
     * Reads the system property {@code puzzle.core.piece.side} to determine which
     * side the piece should run on. Defaults to {@code CLIENT} if not specified,
     * then invokes {@link PrePiece#launch(String[], String)} to start the piece.
     * </p>
     *
     * @param args the command-line arguments passed to the program
     */
    public static void main(String[] args) {
        String side = System.getProperty("puzzle.core.piece.side");
        if (side == null) side = "CLIENT";

        PrePiece.launch(args, side);
    }

}
