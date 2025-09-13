package dev.puzzleshq.puzzleloader.loader.mod.entrypoint.client;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface ClientPostModInit {

    String ENTRYPOINT_KEY = "client-init";

    void onClientPostInit();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                ClientPostModInit.class,
                ClientPostModInit::onClientPostInit
        );
    }

}
