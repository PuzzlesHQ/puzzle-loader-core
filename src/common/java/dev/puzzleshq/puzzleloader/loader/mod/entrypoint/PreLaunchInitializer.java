package dev.puzzleshq.puzzleloader.loader.mod.entrypoint;

import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;

public interface PreLaunchInitializer {
    String ENTRYPOINT_KEY = "preLaunch";

    void onPreLaunch();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                PreLaunchInitializer.class,
                PreLaunchInitializer::onPreLaunch
        );
    }
}
