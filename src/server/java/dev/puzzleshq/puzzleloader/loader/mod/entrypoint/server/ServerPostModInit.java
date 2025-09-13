package dev.puzzleshq.puzzleloader.loader.mod.entrypoint.server;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface ServerPostModInit {
    String ENTRYPOINT_KEY = "server-postInit";

    void onServerPostInit();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                ServerPostModInit.class,
                ServerPostModInit::onServerPostInit
        );
    }
}
