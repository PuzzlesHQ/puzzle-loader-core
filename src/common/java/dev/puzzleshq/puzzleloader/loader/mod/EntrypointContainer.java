package dev.puzzleshq.puzzleloader.loader.mod;

import dev.puzzleshq.mod.api.IEntrypointContainer;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.util.EntrypointPair;
import dev.puzzleshq.puzzleloader.loader.provider.lang.ILangProvider;
import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointInstantiator;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public class EntrypointContainer implements IEntrypointContainer {

    private final IModContainer container;
    private final Map<String, EntrypointPair[]> entrypointMap;

    public EntrypointContainer(IModContainer container) {
        this.container = container;
        this.entrypointMap = container.getInfo().getEntrypointMap();
    }

    @Override
    public <T> void invoke(String key, Class<T> type, Consumer<? super T> invoker) {
        ILangProvider.init();

        Object[] objects = PuzzleEntrypointInstantiator.getEntryPointInstances(this, key);

        for (Object object : objects) {
            Class<?> objectClass = object.getClass();
            if (!type.isAssignableFrom(objectClass)) continue;

            PuzzleEntrypointInstantiator.tryInvoke(object, type, invoker);
        }
    }

    @Override
    public @Nullable EntrypointPair[] getEntrypoints(String s) {
        return entrypointMap.get(s);
    }

    @Override
    public Map<String, EntrypointPair[]> getEntrypointMap() {
        return entrypointMap;
    }

    @Override
    public IModContainer getContainer() {
        return container;
    }
}
