package dev.puzzleshq.puzzleloader.loader.mod.entrypoint.common;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface PreModInit {

    String ENTRYPOINT_KEY = "preInit";

    void onPreInit();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                PreModInit.class,
                PreModInit::onPreInit
        );
    }

}
