package com.github.puzzle.loader.launch.pieces;

import com.github.puzzle.loader.launch.Piece;
import com.github.puzzle.loader.util.EnvType;

public class ServerPiece {

    public static void main(String[] args) {
        Piece.launch(args, EnvType.SERVER);
    }

}
