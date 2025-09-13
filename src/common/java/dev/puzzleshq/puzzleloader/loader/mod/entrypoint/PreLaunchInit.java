package dev.puzzleshq.puzzleloader.loader.mod.entrypoint;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface PreLaunchInit {
    String ENTRYPOINT_KEY = "preLaunch";

    void onPreLaunch();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                PreLaunchInit.class,
                PreLaunchInit::onPreLaunch
        );
    }
}
