package dev.puzzleshq.puzzleloader.loader.provider.game.impl;

import com.github.villadora.semver.Version;
import dev.puzzleshq.mod.info.ModInfoBuilder;
import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mod.ModContainer;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.hjson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class CosmicReachProvider implements IGameProvider {

    public static final String PARADOX_SERVER_ENTRYPOINT = "com.github.puzzle.paradox.loader.launch.PuzzlePiece";
    public static final String PARADOX_SERVER_ENTRYPOINT_CLASS = "com/github/puzzle/paradox/loader/launch/PuzzlePiece.class";

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
        return Version.valueOf(getRawVersion());
    }

    String rawVersion;

    @Override
    public String getRawVersion() {
        if (rawVersion != null) return rawVersion;
        rawVersion = RawAssetLoader.getLowLevelClassPathAssetErrors("build_assets/version.txt", false).getString().replaceAll("[A-Za-z]", "");

        process();
        return rawVersion;
    }

    private void process() {
        rawVersion = rawVersion.replaceAll("\\+.*", "");
        String[] parts = rawVersion.split("-");
        if (parts.length == 3) {
            rawVersion = rawVersion.replaceAll("-pre\\d+-", "");
            rawVersion += "." + parts[1].replaceAll("pre", "");
        }
    }

    AtomicReference<Boolean> paradoxExist = new AtomicReference<>();

    boolean isRunningOnParadox() {
        if (paradoxExist.get() != null) return paradoxExist.get();
        String property = System.getProperty("puzzle.useParadox");
        if (property != null) {
            boolean paradoxExist = Boolean.parseBoolean(property);
            this.paradoxExist.set(paradoxExist);
            return paradoxExist;
        }

        try {
            RawAssetLoader.getLowLevelClassPathAssetErrors(PARADOX_SERVER_ENTRYPOINT_CLASS, false).dispose();
            paradoxExist.set(true);
            return true;
        } catch (NullPointerException ignore) {
            paradoxExist.set(false);
            return false;
        }
    }

    @Override
    public String getEntrypoint() {
        if (LoaderConstants.SIDE == EnvType.SERVER) {
            return isRunningOnParadox() ? CosmicReachProvider.PARADOX_SERVER_ENTRYPOINT : "finalforeach.cosmicreach.server.ServerLauncher";
        }

        return "finalforeach.cosmicreach.lwjgl3.Lwjgl3Launcher";
    }

    public Collection<String> getArgs() {
        return Arrays.asList(args);
    }

    String[] args;

    @Override
    public void initArgs(String[] args) {
        this.args = args;
    }

    @Override
    public void addBuiltinMods() {
        ModInfoBuilder cosmicModInfo = new ModInfoBuilder();
        {
            cosmicModInfo.setDisplayName(getName());
            cosmicModInfo.setId(getId());
            cosmicModInfo.setDescription("The base game.");
            cosmicModInfo.addAuthor("FinalForEach");
            cosmicModInfo.setVersion(getGameVersion().toString());
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
        try {
            String launcher = isRunningOnParadox() ? CosmicReachProvider.PARADOX_SERVER_ENTRYPOINT_CLASS : "finalforeach/cosmicreach/server/ServerLauncher.class";
            if (Piece.getSide() == EnvType.SERVER) {
                try {
                    RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
                    return true;
                } catch (Exception ignore) {
                    throw new RuntimeException("Cosmic Reach Server Main does not exist.");
                }
            }
            try {
                launcher = "finalforeach/cosmicreach/lwjgl3/Lwjgl3Launcher.class";
                RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
            } catch (Exception e) {
                throw new RuntimeException("Cosmic Reach Main does not exist.");
            }
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

}
