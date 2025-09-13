package dev.puzzleshq.puzzleloader.loader.mod.entrypoint.server;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface ServerModInit {
    String ENTRYPOINT_KEY = "server-init";

    void onServerInit();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                ServerModInit.class,
                ServerModInit::onServerInit
        );
    }
}
