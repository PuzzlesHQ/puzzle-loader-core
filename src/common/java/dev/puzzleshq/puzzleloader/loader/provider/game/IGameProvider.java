package dev.puzzleshq.puzzleloader.loader.provider.game;

import com.github.zafarkhaja.semver.Version;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.IFlexClassloader;

import java.util.Collection;

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

    // Init
    default void registerTransformers(IFlexClassloader classLoader) {}
    default void initArgs(String[] args) {}
    default void inject(IFlexClassloader classLoader) {}

    void addBuiltinMods();

    String getDefaultNamespace();

    boolean isValid();
}
