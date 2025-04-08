package dev.puzzleshq.loader.provider.game;

import dev.puzzleshq.loader.launch.PieceClassLoader;
import dev.puzzleshq.loader.util.Version;

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
    void registerTransformers(PieceClassLoader classLoader);
    void initArgs(String[] args);
    void inject(PieceClassLoader classLoader);

    void addBuiltinMods();

    String getDefaultNamespace();

    boolean isValid();
}
