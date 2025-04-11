package dev.puzzleshq.loader.provider.game.impl;

import com.github.zafarkhaja.semver.Version;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import dev.puzzleshq.loader.Constants;
import dev.puzzleshq.loader.launch.Piece;
import dev.puzzleshq.loader.launch.PieceClassLoader;
import dev.puzzleshq.loader.mod.ModContainer;
import dev.puzzleshq.loader.mod.info.ModInfo;
import dev.puzzleshq.loader.provider.game.IGameProvider;
import dev.puzzleshq.loader.util.EnvType;
import dev.puzzleshq.loader.util.ModFinder;
import dev.puzzleshq.loader.util.RawAssetLoader;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.spongepowered.asm.mixin.Mixins;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CosmicReachProvider implements IGameProvider {

    public static final String PARADOX_SERVER_ENTRYPOINT = "com.github.puzzle.paradox.loader.launch.PuzzlePiece";

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
            if (System.getProperty("puzzle.useParadox") != null) {
                paradoxExist.set(true);
                return true;
            }

            try {
                Class.forName(CosmicReachProvider.PARADOX_SERVER_ENTRYPOINT, false, Piece.classLoader);
                paradoxExist.set(true);
                return true;
            } catch (ClassNotFoundException ignore) {
                paradoxExist.set(false);
                return false;
            }
        }).get();

        if (Constants.SIDE == EnvType.SERVER) {
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
        List<Pair<EnvType, String>> mixinConfigs = new ArrayList<>();
        for (ModContainer mod : ModFinder.getModsArray()) {
            if (!mod.INFO.MixinConfigs.isEmpty()) mixinConfigs.addAll(mod.INFO.MixinConfigs);
        }

        EnvType envType = Constants.SIDE;
        mixinConfigs.forEach((e) -> {
            if (envType == e.getKey() || e.getKey() == EnvType.UNKNOWN) {
                Mixins.addConfiguration(e.getRight());
            }
        });
    }

    @Override
    public void addBuiltinMods() {
        ModInfo.Builder cosmicModInfo = ModInfo.Builder.New();
        {
            cosmicModInfo.setName(getName());
            cosmicModInfo.setId("cosmic-reach");
            cosmicModInfo.setDesc("The base game.");
            cosmicModInfo.addAuthor("FinalForEach");
            cosmicModInfo.setVersion(getGameVersion());
            HashMap<String, JsonValue> meta = new HashMap<>();
            meta.put("icon", JsonObject.valueOf("icons/logox256.png"));
            cosmicModInfo.setMeta(meta);
            ModFinder.addModWithContainer(cosmicModInfo.build().getOrCreateModContainer());
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
