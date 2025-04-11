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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.spongepowered.asm.mixin.Mixins;

import java.util.*;

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
//        return Version.parse(version);
        return Version.parse("1.0.0");
    }

    @Override
    public String getRawVersion() {
//        return version;
        return "1.0.0";
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
                args.add(0, launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", ""));
                args.add(0, "--puzzleEdition");
            }
        }

        return launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", "");
    }

    String version = "";

    @Override
    public Collection<String> getArgs() {
        return args;
    }

    @Override
    public void registerTransformers(PieceClassLoader classLoader) {}

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
            ModFinder.addModWithContainer(minecraftModInfo.build().getOrCreateModContainer());
        }

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
