package dev.puzzleshq.loader.util;

import java.util.function.Consumer;

public class PuzzleEntrypointUtil {

    public static <T> void invoke(String key, Class<T> entrypointType, Consumer<? super T> entrypointInvoker) {
        ModFinder.getModsArray().forEach(c -> {
            try {
                c.getEntrypointContainer().invoke(key, entrypointType, entrypointInvoker);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}