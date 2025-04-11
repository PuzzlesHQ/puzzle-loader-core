package dev.puzzleshq.loader.mod;

import dev.puzzleshq.loader.Constants;
import dev.puzzleshq.loader.launch.Piece;
import dev.puzzleshq.loader.mod.info.AdapterPathPair;
import dev.puzzleshq.loader.provider.ProviderException;
import dev.puzzleshq.loader.provider.lang.ILangProvider;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.include.com.google.common.collect.ImmutableCollection;
import org.spongepowered.include.com.google.common.collect.ImmutableMap;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class EntrypointContainer {
    public final ImmutableMap<String, ImmutableCollection<AdapterPathPair>> entrypointClasses;
    public final ModContainer container;

    public static final Map<String, Object> INSTANCE_MAP = new HashMap<>();

    public <T> void invokeClasses(String key, Class<T> type, Consumer<? super T> invoker) throws Exception {
        if (!ILangProvider.PROVDERS.containsKey("java"))
            ILangProvider.PROVDERS.put("java", ILangProvider.JAVA_INSTANCE);

        ImmutableCollection<AdapterPathPair> pairImmutableCollection = entrypointClasses.get(key);
        if (pairImmutableCollection != null) {
            for (AdapterPathPair pair : pairImmutableCollection){
                if (ILangProvider.PROVDERS.get(pair.getAdapter()) == null)
                    throw new ProviderException("LangProvider \"" + pair.getAdapter() + "\" does not exist.");

                T inst = (T) EntrypointContainer.INSTANCE_MAP.get(pair.getValue());

                if (inst == null) {
                    Class<?> instClass = null;
                    try {
                        instClass = Piece.classLoader.findClass(pair.getValue());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    Constants.EVENT_BUS.registerLambdaFactory(instClass.getPackage().getName(), (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
                    inst = ILangProvider.PROVDERS.get(pair.getAdapter()).create(container.INFO, pair.getValue(), type);
                    EntrypointContainer.INSTANCE_MAP.put(pair.getValue(), inst);
                }
                invoker.accept(inst);
            }
        }
    }

    public EntrypointContainer(ModContainer container, @NotNull ImmutableMap<String, ImmutableCollection<AdapterPathPair>> entrypoints) {
        this.container = container;
        entrypointClasses = entrypoints;
    }

}
