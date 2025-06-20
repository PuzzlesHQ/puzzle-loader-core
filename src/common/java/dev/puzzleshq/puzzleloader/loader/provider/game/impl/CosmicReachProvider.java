package dev.puzzleshq.puzzleloader.loader.provider.game.impl;

import com.github.zafarkhaja.semver.Version;
import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mod.ModContainer;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import dev.puzzleshq.mod.info.ModInfoBuilder;
import org.hjson.JsonObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CosmicReachProvider implements IGameProvider {

    public static final String PARADOX_SERVER_ENTRYPOINT = "com.github.puzzle.paradox.loader.launch.PuzzlePiece";
    public static final String PARADOX_SERVER_ENTRYPOINT_CLASS = "/com/github/puzzle/paradox/loader/launch/PuzzlePiece.class";

    public CosmicReachProvider() {
        Piece.provider = this;
    }

    @Override
    public String getId() {
        return "cosmic-reach";
    }

    @Override
    public String getName() {
        return "Cosmic Reach";
    }

    @Override
    public Version getGameVersion() {
        return Version.parse(getRawVersion());
    }

    String rawVersion;

    @Override
    public String getRawVersion() {
        if (rawVersion != null) return rawVersion;
        return rawVersion = RawAssetLoader.getLowLevelClassPathAsset("/build_assets/version.txt").getString();
    }

    AtomicReference<Boolean> paradoxExist = new AtomicReference<>();

    @Override
    public String getEntrypoint() {
        boolean isRunningOnParadox = ((Supplier<Boolean>) () -> {
            if (paradoxExist.get() != null) return paradoxExist.get();
            String property = System.getProperty("puzzle.useParadox");
            if (property != null) {
                boolean paradoxExist = Boolean.valueOf(property);
                this.paradoxExist.set(paradoxExist);
                return paradoxExist;
            }

            try {
                RawAssetLoader.getLowLevelClassPathAsset(PARADOX_SERVER_ENTRYPOINT_CLASS).dispose();
                paradoxExist.set(true);
                return true;
            } catch (NullPointerException ignore) {
                paradoxExist.set(false);
                return false;
            }
        }).get();

        if (LoaderConstants.SIDE == EnvType.SERVER) {
            return isRunningOnParadox ? CosmicReachProvider.PARADOX_SERVER_ENTRYPOINT : "finalforeach.cosmicreach.server.ServerLauncher";
        }

        return "finalforeach.cosmicreach.lwjgl3.Lwjgl3Launcher";
    }

    public Collection<String> getArgs() {
        return Arrays.asList(args);
    }

    @Override
    public void registerTransformers(PieceClassLoader classLoader) {}

    String[] args;

    @Override
    public void initArgs(String[] args) {
        this.args = args;
    }

    @Override
    public void inject(PieceClassLoader classLoader) {
    }

    @Override
    public void addBuiltinMods() {
        ModInfoBuilder cosmicModInfo = new ModInfoBuilder();
        {
            cosmicModInfo.setDisplayName(getName());
            cosmicModInfo.setId(getId());
            cosmicModInfo.setDescription("The base game.");
            cosmicModInfo.addAuthor("FinalForEach");
            cosmicModInfo.setVersion(getRawVersion());
            cosmicModInfo.addMeta("icon", JsonObject.valueOf("icons/logox256.png"));
            ModFinder.addModWithContainer(new ModContainer(cosmicModInfo.build()));
        }
    }

    @Override
    public String getDefaultNamespace() {
        return "base";
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
