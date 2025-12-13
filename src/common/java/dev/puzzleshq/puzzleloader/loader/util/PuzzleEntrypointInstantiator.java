package dev.puzzleshq.puzzleloader.loader.util;

import dev.puzzleshq.annotation.Internal;
import dev.puzzleshq.annotation.documentation.Documented;
import dev.puzzleshq.mod.api.IEntrypointContainer;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.mod.util.EntrypointPair;
import dev.puzzleshq.puzzleloader.loader.provider.ProviderException;
import dev.puzzleshq.puzzleloader.loader.provider.lang.ILangProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * This utility class creates or and finds instances of entrypoint classes from mods.
 *
 * @author Mr_Zombii
 * @since 1.4.0
 */
@Documented
public class PuzzleEntrypointInstantiator {

    // This is a CLASS, INSTANCE pair
    private static final Map<String, Object> ENTRYPOINT_OBJECT_INSTANCES = new HashMap<>();
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private static final Logger LOGGER = LogManager.getLogger("Puzzle Entrypoint Instantiator");

    /**
     * Get all instances of an entrypoint named "entryPointKey" from an "entrypointContainer".
     *
     * @param entrypointContainer The entrypoint container to search.
     * @param entryPointKey The given entrypoint you want to get instances of.
     * @return instances from an entrypoint key without a specific type.
     */
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
                warnClass(className, adapter);
            }
        }

        return objectList.toArray();
    }

    /**
     * Get all instances of an entrypoint named "entryPointKey" from an "entrypointContainer".
     *
     * @param modContainer The mod container to search.
     * @param entryPointKey The given entrypoint you want to get instances of.
     * @return instances from an entrypoint key without a specific type.
     */
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
                warnClass(className, adapter);
            }
        }
    }

    /**
     * Pre instantiates all the entrypoint instances. Used in a {@link AppProxyClassUtil}
     */
    public static void createAllModEntryPointInstances() {
        for (IModContainer iModContainer : ModFinder.getModsArray()) {
            createEntryPointInstances(iModContainer);
        }
    }

    /**
     * Gets existing instance or creates instance of an entrypoint when given a {@link EntrypointPair}
     * @param modInfo The modJson info from a given mod.
     * @param pair The entrypoint adapter/class pair.
     * @return An instance of an entrypoint at pair, could be null if it wasn't able to instance it.
     */
    public static @Nullable Object getOrCreate(ModInfo modInfo, EntrypointPair pair) {
        String adapter = pair.adapter();
        String className = pair.entrypoint();

        Object instance = ENTRYPOINT_OBJECT_INSTANCES.get(className);
        if (instance != null) return instance;

        ILangProvider provider = ILangProvider.PROVDERS.get(adapter);

        try {
            Object newInstance = provider.create(modInfo, className, AnyObject.class);

            ENTRYPOINT_OBJECT_INSTANCES.put(className, newInstance);
            return newInstance;
        } catch (RuntimeException | ProviderException ignored) {
            // this prevents a giant crash, print error for skipped entry points
            warnClass(className, adapter);
            return null;
        }
    }

    /**
     * Tries to invoke an entrypoint, skips if it fails.
     *
     * @param instance Entrypoint instance
     * @param entrypointType The expected entrypoint type
     * @param invoker The invoker that would launch if instance is of type "entrypointType"
     * @return false if it failed, true if it succeeded
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean tryInvoke(Object instance, Class<T> entrypointType, Consumer<? super T> invoker) {
        if (entrypointType.isAssignableFrom(instance.getClass())) {
            invoker.accept((T) instance);
            return true;
        }
        return false;
    }

    @Internal
    private static void warnClass(String className, String adapter) {
        LOGGER.warn("Could not instantiate entrypoint class: \"{}\" for adapter: \"{}\", skipping either broken class or class from wrong side.", className, adapter);
    }

}
