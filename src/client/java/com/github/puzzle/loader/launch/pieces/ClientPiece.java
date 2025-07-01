package com.github.puzzle.loader.launch.pieces;

import dev.puzzleshq.annotation.documentation.Note;

@Deprecated(since = "core-1.0.0", forRemoval = true)
@Note("Being kept for Launcher Compat *for now*")
public class ClientPiece {

    public static void main(String[] args) {
        dev.puzzleshq.puzzleloader.loader.launch.pieces.ClientPiece.main(args);
    }

}
