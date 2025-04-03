package com.github.puzzle.loader.provider.game;

import com.github.puzzle.loader.launch.PuzzleClassLoader;
import com.github.puzzle.loader.util.Version;

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

    // Inits
    void registerTransformers(PuzzleClassLoader classLoader);
    void initArgs(String[] args);
    void inject(PuzzleClassLoader classLoader);

    void addBuiltinMods();

    String getDefaultNamespace();

    boolean isValid();
}
