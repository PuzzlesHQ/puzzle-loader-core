package dev.puzzleshq.loader.mod.entrypoint;

import dev.puzzleshq.loader.util.PuzzleEntrypointUtil;

public interface PreLaunchInitializer {
    String ENTRYPOINT_KEY = "prelaunch";

    void onPreLaunch();

    static void invoke() {
        PuzzleEntrypointUtil.invoke(
                ENTRYPOINT_KEY,
                PreLaunchInitializer.class,
                PreLaunchInitializer::onPreLaunch
        );
    }
}
