package dev.puzzleshq.puzzleloader.loader.provider.game.impl;

import com.github.villadora.semver.Version;
import dev.puzzleshq.mod.info.ModInfoBuilder;
import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.mod.ModContainer;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.hjson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
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

    String name = "Cosmic Reach";

    @Override
    public String getName() {
        return name;
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
            name = "Puzzle Paradox";
            return true;
        } catch (NullPointerException ignore) {
            paradoxExist.set(false);
            return false;
        }
    }

    @Override
    public String getEntrypoint() {
        if (Piece.getSide() == EnvType.SERVER) {
            return isRunningOnParadox() ? CosmicReachProvider.PARADOX_SERVER_ENTRYPOINT : "finalforeach.cosmicreach.server.ServerLauncher";
        }

        return "finalforeach.cosmicreach.lwjgl3.Lwjgl3Launcher";
    }

    String paradoxVersion = "";

    @Override
    public String getVisibleVersion() {
        if (isRunningOnParadox()) {
            try {
                return paradoxVersion = Class.forName("com.github.puzzle.paradox.core.PuzzlePL", false, Piece.classLoader).getPackage().getImplementationVersion();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return getRawVersion();
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
            cosmicModInfo.setDisplayName("Cosmic Reach");
            cosmicModInfo.setId(getId());
            cosmicModInfo.setDescription("The base game.");
            cosmicModInfo.addAuthor("FinalForEach");
            cosmicModInfo.setVersion(getGameVersion().toString());
            cosmicModInfo.addMeta("icon", JsonObject.valueOf("icons/logox256.png"));
            ModFinder.addModWithContainer(new ModContainer(cosmicModInfo.build()));
        }

        if (isRunningOnParadox()) {
            ModInfoBuilder paradoxModInfo = new ModInfoBuilder();
            {
                paradoxModInfo.setDisplayName("Paradox");
                paradoxModInfo.setId("puzzle-paradox");
                paradoxModInfo.setDescription("The Paradox Plugin Loader");
                paradoxModInfo.addAuthor("Replet");
                paradoxModInfo.setVersion(getVisibleVersion());
                ModFinder.addModWithContainer(new ModContainer(paradoxModInfo.build()));
            }
        }
    }

    @Override
    public String getDefaultNamespace() {
        return "base";
    }

    boolean isValid = false;
    String validClass;

    @Override
    public boolean isValid() {
        if (isValid) return true;

        String launcher = "finalforeach/cosmicreach/lwjgl3/Lwjgl3Launcher.class";
        if (Piece.getSide() == EnvType.SERVER) launcher = "finalforeach/cosmicreach/server/ServerLauncher.class";
        try {
            RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
            validClass = launcher;
            return isValid = true;
        } catch (Exception ignore) {}
        return isValid = false;
    }

    @Override
    public @Nullable URL getJarLocation() {
        if (LoaderConstants.CLIConfiguration.PATCH_PAMPHLET_FILE == null) return null;

        URL url = RawAssetLoader.getLowLevelClassPathUrl(validClass);
        try {
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection) {
                JarURLConnection jarURLConnection = ((JarURLConnection) connection);

                return jarURLConnection.getJarFileURL();
            } else throw new RuntimeException("HoW?!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
