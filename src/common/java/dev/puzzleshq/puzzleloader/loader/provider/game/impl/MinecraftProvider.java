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
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.hjson.JsonObject;

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
        String launcher = "net/minecraft/server/Main.class";
        if (LoaderConstants.SIDE == EnvType.SERVER) {
            try {
                RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
                return launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", "");
            } catch (Exception ignore) {
                throw new RuntimeException("Minecraft Server Main does not exist.");
            }
        }
        try {
            launcher = "net/minecraft/client/main/Main.class";
            RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
        } catch (Exception e) {
            try {
                launcher = "net/minecraft/client/MinecraftApplet.class";
                RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
            } catch (Exception a) {
                try {
                    launcher = "com/mojang/MinecraftApplet.class";
                    RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
                } catch (Exception sports) {
                    try {
                        launcher = "com/mojang/rubydung/RubyDung.class";
                        RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
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

    }

    @Override
    public void addBuiltinMods() {
        ModInfoBuilder minecraftModInfo = new ModInfoBuilder();
        {
            minecraftModInfo.setDisplayName(getName());
            minecraftModInfo.setId(getId());
            minecraftModInfo.setDescription("The base game.");
            minecraftModInfo.addAuthor("Mojang");
            minecraftModInfo.setVersion(getRawVersion());
            minecraftModInfo.addMeta("icon", JsonObject.valueOf("pack.png"));
            ModFinder.addModWithContainer(new ModContainer(minecraftModInfo.build()));
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
            if (LoaderConstants.SIDE == EnvType.SERVER) {
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
