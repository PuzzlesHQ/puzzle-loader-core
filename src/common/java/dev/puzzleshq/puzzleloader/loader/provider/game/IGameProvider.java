package dev.puzzleshq.puzzleloader.loader.provider.game;

import com.github.zafarkhaja.semver.Version;
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
    void registerTransformers(PieceClassLoader classLoader);
    void initArgs(String[] args);
    void inject(PieceClassLoader classLoader);

    void addBuiltinMods();

    String getDefaultNamespace();

    boolean isValid();
}
