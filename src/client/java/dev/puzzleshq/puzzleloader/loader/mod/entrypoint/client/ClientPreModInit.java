package dev.puzzleshq.puzzleloader.loader.mod.entrypoint.client;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface ClientPreModInit {

    String ENTRYPOINT_KEY = "client-preInit";

    void onClientPreInit();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                ClientPreModInit.class,
                ClientPreModInit::onClientPreInit
        );
    }

}
