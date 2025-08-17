package dev.puzzleshq.puzzleloader.loader.provider.game;

import com.github.villadora.semver.Version;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;

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
    void initArgs(String[] args);
    default void registerTransformers(PieceClassLoader classLoader) {}
    default void inject(PieceClassLoader classLoader) {}

    void addBuiltinMods();

    String getDefaultNamespace();

    boolean isValid();
}
