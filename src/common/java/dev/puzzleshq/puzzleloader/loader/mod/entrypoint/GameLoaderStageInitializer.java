package dev.puzzleshq.puzzleloader.loader.mod.entrypoint;

import dev.puzzleshq.puzzleloader.loader.loading.events.EventRegisterStages;
import dev.puzzleshq.puzzleloader.loader.loading.events.EventGameLoaderFinish;

public interface GameLoaderStageInitializer {

    String ENTRYPOINT_KEY = "gameLoaderStageInit";

    default void onGameLoadingStageRegister(EventRegisterStages event) {}
    default void onGameLoadingFinish(EventGameLoaderFinish event) {}

}
