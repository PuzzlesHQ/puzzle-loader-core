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
        Collection<Entrypoint<T>> entrypoints = new ArrayList<>();

        for (IModContainer c : ModFinder.getModsArray()) {
            IEntrypointContainer container = c.getEntrypointContainer();
            EntrypointPair[] pairs = container.getEntrypoints(key);
            if (pairs == null) continue;

            for (EntrypointPair pair : pairs) entrypoints.add(new Entrypoint<>(c, key, entrypointType, pair));
        }

        return entrypoints;
    }

    public static <T> Collection<T> getEntrypointInstances(String key, Class<T> entrypointType) throws ProviderException {
        Collection<T> entrypoints = new ArrayList<>();

        for (IModContainer c : ModFinder.getModsArray()) {
            IEntrypointContainer container = c.getEntrypointContainer();
            EntrypointPair[] pairs = container.getEntrypoints(key);
            if (pairs == null) continue;

            for (EntrypointPair pair : pairs) {
                ILangProvider provider = ILangProvider.PROVDERS.get(pair.adapter());
                entrypoints.add(provider.create(c.getInfo(), pair.entrypoint(), entrypointType));
            }
        }

        return entrypoints;
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

        public T createInstance() throws ProviderException {
            ILangProvider provider = ILangProvider.PROVDERS.get(pair.adapter());
            return provider.create(this.provider.getInfo(), pair.entrypoint(), assumedType);
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