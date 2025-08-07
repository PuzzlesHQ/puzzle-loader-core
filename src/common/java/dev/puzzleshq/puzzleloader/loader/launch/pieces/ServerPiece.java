package dev.puzzleshq.puzzleloader.loader.launch.pieces;

import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapPiece;

public class ServerPiece {

    public static void main(String[] args) {
        BootstrapPiece.launch(args, "SERVER");
//        PrePiece.launch(args, EnvType.SERVER);
    }

}
