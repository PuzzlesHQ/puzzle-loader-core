package dev.puzzleshq.puzzleloader.loader.util;

import dev.puzzleshq.mod.api.IEntrypointContainer;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.mod.util.EntrypointPair;
import dev.puzzleshq.puzzleloader.loader.provider.ProviderException;
import dev.puzzleshq.puzzleloader.loader.provider.lang.ILangProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;

public class PuzzleEntrypointInstantiator {

    // This is a CLASS, INSTANCE pair
    private static final Map<String, Object> ENTRYPOINT_OBJECT_INSTANCES = new HashMap<>();
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private static final Logger LOGGER = LogManager.getLogger("Puzzle Entrypoint Instantiator");

    public static Object[] getEntryPointInstances(IEntrypointContainer entrypointContainer, String entryPointKey) {
        ILangProvider.init();

        EntrypointPair[] pairs = entrypointContainer.getEntrypoints(entryPointKey);
        if (pairs == null || pairs.length == 0)
            return EMPTY_OBJECT_ARRAY;

        ModInfo modInfo = entrypointContainer.getContainer().getInfo();

        List<Object> objectList = new ArrayList<>();
        for (EntrypointPair pair : pairs) {
            String adapter = pair.adapter();
            String className = pair.entrypoint();

            Object instance = ENTRYPOINT_OBJECT_INSTANCES.get(className);
            if (instance != null) {
                objectList.add(instance);
                continue;
            }

            ILangProvider provider = ILangProvider.PROVDERS.get(adapter);

            try {
                Object newInstance = provider.create(modInfo, className, AnyObject.class);

                objectList.add(newInstance);
                ENTRYPOINT_OBJECT_INSTANCES.put(className, newInstance);
            } catch (RuntimeException | ProviderException ignored) {
                error(className, adapter);
            }
        }

        return objectList.toArray();
    }

    private static void error(String className, String adapter) {
        LOGGER.error("Could not instantiate entrypoint class: \"{}\" for adapter: \"{}\", skipping either broken class or class from wrong side.", className, adapter);
    }

    public static Object[] getEntryPointInstances(IModContainer modContainer, String entryPointKey) {
        ILangProvider.init();

        return getEntryPointInstances(modContainer.getEntrypointContainer(), entryPointKey);
    }

    public static void createEntryPointInstances(IModContainer modContainer) {
        ILangProvider.init();

        IEntrypointContainer entrypointContainer = modContainer.getEntrypointContainer();
        Map<String, EntrypointPair[]> pairMap = entrypointContainer.getEntrypointMap();
        Set<String> entryPoints = pairMap.keySet();

        Queue<EntrypointPair> allPairs = new ArrayDeque<>();
        for (String entryPoint : entryPoints) {
            EntrypointPair[] pairs = pairMap.get(entryPoint);
            allPairs.addAll(Arrays.asList(pairs));
        }

        while (!allPairs.isEmpty()) {
            EntrypointPair pair = allPairs.poll();

            String adapter = pair.adapter();
            String className = pair.entrypoint();

            Object instance = ENTRYPOINT_OBJECT_INSTANCES.get(className);
            if (instance != null) continue;

            ILangProvider provider = ILangProvider.PROVDERS.get(adapter);

            try {
                Object newInstance = provider.create(modContainer.getInfo(), className, AnyObject.class);

                ENTRYPOINT_OBJECT_INSTANCES.put(className, newInstance);
            } catch (RuntimeException | ProviderException ignored) {
                // this prevents a giant crash, print error for skipped entry points
                error(className, adapter);
            }
        }
    }

    public static void createAllModEntryPointInstances() {
        for (IModContainer iModContainer : ModFinder.getModsArray()) {
            createEntryPointInstances(iModContainer);
        }
    }

    public static Object getOrCreate(ModInfo container, EntrypointPair pair) {
        String adapter = pair.adapter();
        String className = pair.entrypoint();

        Object instance = ENTRYPOINT_OBJECT_INSTANCES.get(className);
        if (instance != null) return instance;

        ILangProvider provider = ILangProvider.PROVDERS.get(adapter);

        try {
            Object newInstance = provider.create(container, className, AnyObject.class);

            ENTRYPOINT_OBJECT_INSTANCES.put(className, newInstance);
            return newInstance;
        } catch (RuntimeException | ProviderException ignored) {
            // this prevents a giant crash, print error for skipped entry points
            error(className, adapter);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean tryInvoke(Object o, Class<T> type, Consumer<? super T> consumer) {
        if (type.isAssignableFrom(o.getClass())) {
            consumer.accept((T) o);
            return true;
        }
        return false;
    }
}
