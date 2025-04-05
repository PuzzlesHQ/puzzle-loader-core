package com.github.puzzle.loader.provider.game.impl;

import com.github.puzzle.loader.Constants;
import com.github.puzzle.loader.launch.Piece;
import com.github.puzzle.loader.launch.PuzzleClassLoader;
import com.github.puzzle.loader.mod.ModContainer;
import com.github.puzzle.loader.mod.entrypoint.CommonTransformerInitializer;
import com.github.puzzle.loader.mod.info.ModInfo;
import com.github.puzzle.loader.provider.game.IGameProvider;
import com.github.puzzle.loader.util.*;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CosmicReachProvider implements IGameProvider {

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
                Class.forName(ModLocator.PARADOX_SERVER_ENTRYPOINT, false, Piece.classLoader);
                paradoxExist.set(true);
                return true;
            } catch (ClassNotFoundException ignore) {
                paradoxExist.set(false);
                return false;
            }
        }).get();

        if (Constants.SIDE == EnvType.SERVER) {
            return isRunningOnParadox ? ModLocator.PARADOX_SERVER_ENTRYPOINT : "finalforeach.cosmicreach.server.ServerLauncher";
        }

        return "finalforeach.cosmicreach.lwjgl3.Lwjgl3Launcher";
    }

    public Collection<String> getArgs() {
        MixinUtil.goToPhase(MixinEnvironment.Phase.DEFAULT);
        return Arrays.asList(args);
    }

    @Override
    public void registerTransformers(PuzzleClassLoader classLoader) {
        ModLocator.getMods(Constants.SIDE, Arrays.asList(classLoader.getURLs()));

        CommonTransformerInitializer.invokeTransformers(classLoader);

        MixinUtil.start();
    }

    String[] args;

    @Override
    public void initArgs(String[] args) {
        this.args = args;
    }

    @Override
    public void inject(PuzzleClassLoader classLoader) {
        ModLocator.verifyDependencies();

        List<Pair<EnvType, String>> mixinConfigs = new ArrayList<>();
        for (ModContainer mod : ModLocator.locatedMods.values()) {
            if (!mod.INFO.MixinConfigs.isEmpty()) mixinConfigs.addAll(mod.INFO.MixinConfigs);
        }

        EnvType envType = Constants.SIDE;
        mixinConfigs.forEach((e) -> {
            if (envType == e.getKey() || e.getKey() == EnvType.UNKNOWN) {
                Mixins.addConfiguration(e.getRight());
            }
        });

        MixinUtil.inject();
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
            ModLocator.addMod(cosmicModInfo.build().getOrCreateModContainer());
        }

        ModInfo.Builder puzzleCoreModInfo = ModInfo.Builder.New();
        {
            puzzleCoreModInfo.setName("Puzzle Core");
            puzzleCoreModInfo.setId("puzzle-loader-core");
            puzzleCoreModInfo.setDesc("The core mod-loading mechanics of puzzle-loader");
            puzzleCoreModInfo.addDependency("cosmic-reach", getRawVersion());

            HashMap<String, JsonValue> meta = new HashMap<>();
            meta.put("icon", JsonObject.valueOf("puzzle-loader:icons/PuzzleLoaderIconx160.png"));
            puzzleCoreModInfo.setMeta(meta);
            puzzleCoreModInfo.setAuthors(new String[]{
                    "Mr-Zombii"
            });

//            puzzleCoreModInfo.addSidedMixinConfigs(
//                    EnvType.UNKNOWN,
//                    "mixins/client/loader_internal.client.mixins.json"
//            );

            puzzleCoreModInfo.setVersion(Constants.PUZZLE_CORE_VERSION);
        }
        ModLocator.addMod(puzzleCoreModInfo.build().getOrCreateModContainer());
        ModLocator.addMod(cosmicModInfo.build().getOrCreateModContainer());
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
