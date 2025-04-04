package com.github.puzzle.loader.provider.game.impl;

import com.github.puzzle.loader.Constants;
import com.github.puzzle.loader.annotation.Note;
import com.github.puzzle.loader.launch.Piece;
import com.github.puzzle.loader.launch.PuzzleClassLoader;
import com.github.puzzle.loader.mod.ModContainer;
import com.github.puzzle.loader.mod.entrypoint.CommonTransformerInitializer;
import com.github.puzzle.loader.mod.info.ModInfo;
import com.github.puzzle.loader.provider.game.IGameProvider;
import com.github.puzzle.loader.util.*;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.*;

@Note("You thought lol, you should see the look on your face. (This thing worked first try when I made it & it supports loading Mixins and Mods)")
public class MinecraftProvider implements IGameProvider {

    public MinecraftProvider() {
        Piece.provider = this;
    }

    @Override
    public String getId() {
        return "minecraft";
    }

    @Override
    public String getName() {
        return "Minecraft";
    }

    @Override
    public Version getGameVersion() {
        return Version.parse(version);
    }

    @Override
    public String getRawVersion() {
        return version;
    }

    @Override
    public String getEntrypoint() {
        String launcher = "/net/minecraft/server/Main.class";
        if (Constants.SIDE == EnvType.SERVER) {
            try {
                RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
                return launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", "");
            } catch (Exception ignore) {
                throw new RuntimeException("Minecraft Server Main does not exist.");
            }
        }
        try {
            launcher = "/net/minecraft/client/main/Main.class";
            RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
        } catch (Exception e) {
            try {
                launcher = "/net/minecraft/client/MinecraftApplet.class";
                RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
            } catch (Exception a) {
                try {
                    launcher = "/com/mojang/MinecraftApplet.class";
                    RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
                } catch (Exception ignore) {
                    throw new RuntimeException("Minecraft Client Main does not exist.");
                }
            }
        }

        if (launcher.contains("MinecraftApplet.class")) {
            if (args != null && !args.contains("--puzzleEdition")) {
                args.add("--puzzleEdition");
                args.add(launcher);
            }
            return "net.minecraft.launch.MinecraftAppletLauncher";
        }

        return launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", "");
    }

    String version = "";

    @Override
    public Collection<String> getArgs() {
        MixinUtil.goToPhase(MixinEnvironment.Phase.DEFAULT);
        return args;
    }

    @Override
    public void registerTransformers(PuzzleClassLoader classLoader) {
        MixinUtil.start();
        ModLocator.getMods(Constants.SIDE, Arrays.asList(classLoader.getURLs()));

        CommonTransformerInitializer.invokeTransformers(classLoader);
    }

    List<String> args;

    @Override
    public void initArgs(String[] args) {
        OptionParser optionparser = new OptionParser();
        optionparser.allowsUnrecognizedOptions();
        OptionSpec<String> versionSpec = optionparser.accepts("version").withRequiredArg().required();
        OptionSet optionSet = optionparser.parse(args);

        version = versionSpec.value(optionSet);

        this.args = new ArrayList<>(Arrays.asList(args));
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
        ModInfo.Builder minecraftModInfo = ModInfo.Builder.New();
        {
            minecraftModInfo.setName(getName());
            minecraftModInfo.setId(getId());
            minecraftModInfo.setDesc("The base game.");
            minecraftModInfo.addAuthor("Mojang");
            minecraftModInfo.setVersion(getGameVersion());
            HashMap<String, JsonValue> meta = new HashMap<>();
            meta.put("icon", JsonObject.valueOf("pack.png"));
            minecraftModInfo.setMeta(meta);
            ModLocator.addMod(minecraftModInfo.build().getOrCreateModContainer());
        }

        ModInfo.Builder puzzleCoreModInfo = ModInfo.Builder.New();
        {
            puzzleCoreModInfo.setName("Puzzle Core");
            puzzleCoreModInfo.setId("puzzle-loader-core");
            puzzleCoreModInfo.setDesc("The core mod-loading mechanics of puzzle-loader");
            puzzleCoreModInfo.addDependency("minecraft", getRawVersion());

            HashMap<String, JsonValue> meta = new HashMap<>();
            meta.put("icon", JsonObject.valueOf("puzzle-loader:icons/PuzzleLoaderIconx160.png"));
            puzzleCoreModInfo.setMeta(meta);
            puzzleCoreModInfo.setAuthors(new String[]{
                    "Mr-Zombii"
            });

            if (Constants.SIDE == EnvType.CLIENT) {
                puzzleCoreModInfo.addEntrypoint("transformers", "com.github.puzzle.loader.transformers.CoreClientTransformers");
            }

            puzzleCoreModInfo.addSidedMixinConfigs(
                    EnvType.UNKNOWN,
                    "mixins/client/loader_internal.client.mixins.json"
            );

            puzzleCoreModInfo.setVersion(Constants.PUZZLE_CORE_VERSION);
        }
        ModLocator.addMod(puzzleCoreModInfo.build().getOrCreateModContainer());
        ModLocator.addMod(minecraftModInfo.build().getOrCreateModContainer());
    }

    @Override
    public String getDefaultNamespace() {
        return "minecraft";
    }

    @Override
    public boolean isValid() {
        try {
            getEntrypoint();
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }
}
