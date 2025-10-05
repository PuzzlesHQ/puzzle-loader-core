package dev.puzzleshq.puzzleloader.loader.provider.game;

import com.github.villadora.semver.Version;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mixin.MixinController;

import java.util.*;

public interface IGameProvider {

    // Game Names
    String getId();
    String getName();

    // Game Version
    Version getGameVersion();
    String getRawVersion();

    // Extra Data
    String getEntrypoint();
    Collection<String> getArgs();

    // Inits
    void initArgs(String[] args);
    default void registerTransformers(PieceClassLoader classLoader) {}
    default void inject(PieceClassLoader classLoader) {}

    void addBuiltinMods();

    String getDefaultNamespace();

    boolean isValid();

    default String getVisibleVersion() {
        return getRawVersion();
    }

    String[] BUILT_IN_PROVIDERS = {
            "dev.puzzleshq.puzzleloader.loader.provider.game.impl.CosmicReachProvider",
            "dev.puzzleshq.puzzleloader.loader.provider.game.impl.MinecraftProvider",
            "dev.puzzleshq.puzzleloader.loader.provider.game.impl.ProjectZomboidProvider"
    };

    static IGameProvider loadProviderFromString(String clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> providerClass = Class.forName(clazz, true, Piece.classLoader);
        IGameProvider instance = (IGameProvider) providerClass.newInstance();
        if (instance.isValid()) return instance;
        return null;
    }

    static IGameProvider findValidProvider() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        IGameProvider instance = findNonDefaultProvider();
        if (instance != null) return instance;

        return findDefaultProvider();
    }

    static IGameProvider findDefaultProvider() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        for (String builtInProvider : BUILT_IN_PROVIDERS) {
            Class<?> providerClass = Class.forName(builtInProvider, true, Piece.classLoader);
            IGameProvider instance = (IGameProvider) providerClass.newInstance();
            if (instance.isValid()) return instance;
        }
        throw new IllegalStateException("Could not find a valid IGameProvider instance relating to the current game or application.");
    }

    static IGameProvider findNonDefaultProvider() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String providerImpl = System.getProperty("puzzle.core.gameProvider");
        if (providerImpl != null && !providerImpl.isEmpty()) {
            Class<?> providerClass = Class.forName(providerImpl, true, Piece.classLoader);
            if (!IGameProvider.class.isAssignableFrom(providerClass)) throw new InstantiationException("\"" + providerClass.getName() + "\" must implement \"" + IGameProvider.class.getName() + "\"");
            IGameProvider instance = (IGameProvider) providerClass.newInstance();
            if (instance.isValid()) return instance;
            throw new IllegalStateException("\"" + providerClass.getName() + "\" returned false when we called the `isValid` method.");
        }

        ServiceLoader<IGameProvider> serviceLoader = ServiceLoader.load(IGameProvider.class, Piece.classLoader);
        List<IGameProvider> providers = new ArrayList<>();
        for (IGameProvider iGameProvider : serviceLoader) providers.add(iGameProvider);

        IGameProvider[] gameProviders = providers.stream().filter(IGameProvider::isValid).toArray(IGameProvider[]::new);
        if (gameProviders.length == 0) return null;
        return gameProviders[0];
    }


    default void startMixins() {
        MixinController.initializeMixins();
        Map<String, String> mixinConfigToModMap = MixinController.registerModMixins();

        MixinController.applyMixinDecorations(mixinConfigToModMap);
        MixinController.applyMixinConstraints();

        MixinController.injectMixins();
    }

}
