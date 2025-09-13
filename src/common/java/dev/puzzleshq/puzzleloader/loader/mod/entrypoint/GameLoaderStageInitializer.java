package dev.puzzleshq.puzzleloader.loader.mod.entrypoint;

import dev.puzzleshq.puzzleloader.loader.loading.events.EventGameLoaderFinish;
import dev.puzzleshq.puzzleloader.loader.loading.events.EventRegisterStages;

public interface GameLoaderStageInitializer {

    String ENTRYPOINT_KEY = "gameLoaderStageInit";

    default void onGameLoadingStageRegister(EventRegisterStages event) {}
    default void onGameLoadingFinish(EventGameLoaderFinish event) {}

}
