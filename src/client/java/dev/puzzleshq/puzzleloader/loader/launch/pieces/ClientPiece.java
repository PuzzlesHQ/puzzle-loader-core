package dev.puzzleshq.puzzleloader.loader.launch.pieces;

import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapPiece;

public class ClientPiece {

    public static void main(String[] args) {
        BootstrapPiece.launch(args, "CLIENT");
//        PrePiece.launch(args, EnvType.CLIENT);
    }

}
