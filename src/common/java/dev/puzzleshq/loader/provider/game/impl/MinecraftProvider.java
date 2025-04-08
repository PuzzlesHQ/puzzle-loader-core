package dev.puzzleshq.loader.provider.game.impl;

import dev.puzzleshq.loader.Constants;
import dev.puzzleshq.loader.annotation.Note;
import dev.puzzleshq.loader.launch.Piece;
import dev.puzzleshq.loader.launch.PieceClassLoader;
import dev.puzzleshq.loader.mod.ModContainer;
import dev.puzzleshq.loader.mod.entrypoint.TransformerInitializer;
import dev.puzzleshq.loader.mod.info.ModInfo;
import dev.puzzleshq.loader.provider.game.IGameProvider;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import dev.puzzleshq.loader.util.*;
import dev.puzzleshq.loader.util.RawAssetLoader;
import dev.puzzleshq.loader.util.Version;
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
                } catch (Exception sports) {
                    try {
                        launcher = "/com/mojang/rubydung/RubyDung.class";
                        RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
                    } catch (Exception ignore) {
                        throw new RuntimeException("Minecraft Client Main does not exist.");
                    }
                }
            }
        }

        if (launcher.contains("MinecraftApplet.class")) {
            if (args != null && !args.contains("--puzzleEdition")) {
                args.addFirst(launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", ""));
                args.addFirst("--puzzleEdition");
            }
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
    public void registerTransformers(PieceClassLoader classLoader) {
        ModLocator.getMods(Constants.SIDE, Arrays.asList(classLoader.getURLs()));
        TransformerInitializer.invokeTransformers(classLoader);

        MixinUtil.start();
        MixinUtil.doInit(new String[0]);
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
    public void inject(PieceClassLoader classLoader) {
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

        ModLocator.addMod(minecraftModInfo.build().getOrCreateModContainer());
    }

    @Override
    public String getDefaultNamespace() {
        return "minecraft";
    }

    @Override
    public boolean isValid() {
        try {
            String launcher = "/net/minecraft/server/Main.class";
            if (Constants.SIDE == EnvType.SERVER) {
                try {
                    RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
                    return true;
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
                    } catch (Exception sports) {
                        try {
                            launcher = "/com/mojang/rubydung/RubyDung.class";
                            RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
                        } catch (Exception ignore) {
                            throw new RuntimeException("Minecraft Client Main does not exist.");
                        }
                    }
                }
            }
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }
}
