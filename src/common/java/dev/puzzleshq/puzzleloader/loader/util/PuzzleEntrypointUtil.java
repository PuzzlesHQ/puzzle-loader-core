package dev.puzzleshq.puzzleloader.loader.util;

import dev.puzzleshq.mod.api.IEntrypointContainer;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.util.EntrypointPair;
import dev.puzzleshq.puzzleloader.loader.provider.ProviderException;
import dev.puzzleshq.puzzleloader.loader.provider.lang.ILangProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class PuzzleEntrypointUtil {

    public static <T> void invoke(String key, Class<T> entrypointType, Consumer<? super T> entrypointInvoker) {
        for (IModContainer c : ModFinder.getModsArray()) {
            try {
                c.getEntrypointContainer().invoke(key, entrypointType, entrypointInvoker);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> Collection<Entrypoint<T>> getEntrypoints(String key, Class<T> entrypointType) {
        Collection<Entrypoint<T>> entrypointList = new ArrayList<>();

        for (IModContainer c : ModFinder.getModsArray()) {
            IEntrypointContainer container = c.getEntrypointContainer();
            EntrypointPair[] pairs = container.getEntrypoints(key);
            if (pairs == null) continue;

            for (EntrypointPair pair : pairs) entrypointList.add(new Entrypoint<>(c, key, entrypointType, pair));
        }

        return entrypointList;
    }

    @SuppressWarnings("unchecked")
    public static <T> Collection<T> getEntrypointInstances(String key, Class<T> entrypointType) {
        Collection<T> entrypointList = new ArrayList<>();

        for (IModContainer c : ModFinder.getModsArray()) {
            IEntrypointContainer container = c.getEntrypointContainer();
            EntrypointPair[] pairs = container.getEntrypoints(key);
            if (pairs == null) continue;

            for (EntrypointPair pair : pairs) {
                Object o = PuzzleEntrypointInstantiator.getOrCreate(c.getInfo(), pair);
                if (o == null) continue;
                if (!entrypointType.isAssignableFrom(o.getClass())) continue;
                entrypointList.add((T) o);
            }
        }

        return entrypointList;
    }

    public static class Entrypoint<T> {

        private final Class<T> assumedType;
        private final EntrypointPair pair;
        private final IModContainer provider;
        private final String key;

        public Entrypoint(IModContainer provider, String key, Class<T> assumedType, EntrypointPair pair) {
            this.provider = provider;
            this.pair = pair;
            this.assumedType = assumedType;
            this.key = key;
        }

        @SuppressWarnings("unchecked")
        public T createInstance() throws ProviderException {
            Object o = PuzzleEntrypointInstantiator.getOrCreate(provider.getInfo(), pair);
            if (o == null || !assumedType.isAssignableFrom(o.getClass())) return null;
            return (T) o;
        }

        public String getKey() {
            return key;
        }

        public Class<T> getAssumedType() {
            return assumedType;
        }

        public EntrypointPair getPair() {
            return pair;
        }

        public IModContainer getProvider() {
            return this.provider;
        }

    }

}