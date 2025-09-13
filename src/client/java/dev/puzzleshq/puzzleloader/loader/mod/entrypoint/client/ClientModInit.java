package dev.puzzleshq.puzzleloader.loader.mod.entrypoint.client;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface ClientModInit {

    String ENTRYPOINT_KEY = "client-init";

    void onClientInit();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                ClientModInit.class,
                ClientModInit::onClientInit
        );
    }

}
