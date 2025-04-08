package dev.puzzleshq.loader.util;

import dev.puzzleshq.loader.Constants;

import java.util.function.Consumer;

public class PuzzleEntrypointUtil {
    public static <T> void invoke(String key, Class<T> entrypointType, Consumer<? super T> entrypointInvoker) {
        ModLocator.getMods(Constants.SIDE);
        ModLocator.locatedMods.values().forEach(modContainer -> {
            try {
                modContainer.invokeEntrypoint(key, entrypointType, entrypointInvoker);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T> void invoke(EnvType env, String key, Class<T> entrypointType, Consumer<? super T> entrypointInvoker) {
        ModLocator.getMods(env);

        ModLocator.locatedMods.values().forEach(modContainer -> {
            try {
                modContainer.invokeEntrypoint(key, entrypointType, entrypointInvoker);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}