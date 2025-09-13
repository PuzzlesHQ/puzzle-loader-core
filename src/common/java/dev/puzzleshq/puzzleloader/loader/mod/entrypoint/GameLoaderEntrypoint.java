package dev.puzzleshq.puzzleloader.loader.mod.entrypoint;

import dev.puzzleshq.puzzleloader.loader.loading.events.EventGameLoaderFinish;
import dev.puzzleshq.puzzleloader.loader.loading.events.EventRegisterStages;

public interface GameLoaderEntrypoint {

    String ENTRYPOINT_KEY = "gameLoaderEntrypoint";

    default void onGameLoadingStageRegister(EventRegisterStages event) {}
    default void onGameLoadingFinish(EventGameLoaderFinish event) {}

}
