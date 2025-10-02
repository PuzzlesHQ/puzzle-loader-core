package dev.puzzleshq.puzzleloader.loader.mod;

import dev.puzzleshq.mod.api.IEntrypointContainer;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.util.EntrypointPair;
import dev.puzzleshq.puzzleloader.loader.provider.ProviderException;
import dev.puzzleshq.puzzleloader.loader.provider.lang.ILangProvider;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EntrypointContainer implements IEntrypointContainer {

    public static final Map<String, Object> INSTANCE_MAP = new HashMap<>();

    private final IModContainer container;
    private final Map<String, EntrypointPair[]> entrypointMap;

    public EntrypointContainer(IModContainer container) {
        this.container = container;
        this.entrypointMap = container.getInfo().getEntrypointMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void invoke(String key, Class<T> type, Consumer<? super T> invoker) {
        ILangProvider.init();

        EntrypointPair[] pairs = getEntrypoints(key);
        if (pairs == null) return;

        for (EntrypointPair pair : pairs) {
            T instance = (T) EntrypointContainer.INSTANCE_MAP.get(pair.entrypoint());

            if (instance != null) {
                invoker.accept(instance);
                continue;
            }

            try {
                ILangProvider provider = ILangProvider.PROVDERS.get(pair.adapter());
                if (provider == null) throw new ProviderException("LangProvider \"" + pair.adapter() + "\" does not exist.");

                instance = provider.create(
                        container.getInfo(),
                        pair.entrypoint(),
                        type
                );

                EntrypointContainer.INSTANCE_MAP.put(pair.entrypoint(), instance);
                invoker.accept(instance);
            } catch (ProviderException e) {
                throw new RuntimeException(e);
            }

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
