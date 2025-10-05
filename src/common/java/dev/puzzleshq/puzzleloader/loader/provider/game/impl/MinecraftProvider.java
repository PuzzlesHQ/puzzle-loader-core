package dev.puzzleshq.puzzleloader.loader.provider.game.impl;

import com.github.villadora.semver.Version;
import dev.puzzleshq.mod.info.ModInfoBuilder;
import dev.puzzleshq.puzzleloader.loader.LoaderConfig;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.mod.ModContainer;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.provider.game.IPatchableGameProvider;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.hjson.JsonObject;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MinecraftProvider implements IGameProvider, IPatchableGameProvider {

    public MinecraftProvider() {
        Piece.gameProvider = this;
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
        return Version.valueOf("1.0.0");
    }

    @Override
    public String getRawVersion() {
//        return version;
        return "1.0.0";
    }

    @Override
    public String getEntrypoint() {
        String launcher = null;

        String[] clientClasses = {
                "net/minecraft/client/MinecraftApplet.class",
                "net/minecraft/client/main/Main.class",
                "com/mojang/rubydung/RubyDung.class",
                "com/mojang/MinecraftApplet.class",
        };

        for (String clientClass : clientClasses) {
            try {
                RawAssetLoader.getLowLevelClassPathAsset(clientClass).dispose();
                launcher = clientClass;
            } catch (Exception ignore) {
                throw new RuntimeException("Minecraft Client Main does not exist.");
            }
        }

        if (launcher.contains("MinecraftApplet.class")) {
            if (args != null && !args.contains("--puzzleEdition")) {
                args.add(0, launcher.replaceAll("/", ".").replace(".class", ""));
                args.add(0, "--puzzleEdition");
            }
        }

        if (Piece.getSide().equals(EnvType.SERVER)) {
            try {
                launcher = "net/minecraft/server/Main.class";
                RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
            } catch (Exception ignore) {
                throw new RuntimeException("Minecraft Server Main does not exist.");
            }
        }

        return launcher.replaceAll("/", ".").replace(".class", "");
    }

    String version = "";

    @Override
    public Collection<String> getArgs() {
        return args;
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

    String validClass;

    @Override
    public boolean isValid() {
        String[] clientClasses = {
                "net/minecraft/client/MinecraftApplet.class",
                "net/minecraft/client/main/Main.class",
                "com/mojang/rubydung/RubyDung.class",
                "com/mojang/MinecraftApplet.class",
        };

        for (String clientClass : clientClasses) {
            try {
                validClass = clientClass;
                RawAssetLoader.getLowLevelClassPathAsset(clientClass).dispose();
                return true;
            } catch (Exception ignore) {
                throw new RuntimeException("Minecraft Client Main does not exist.");
            }
        }

        if (!Piece.getSide().equals(EnvType.SERVER)) return false;

        try {
            validClass = "net/minecraft/server/Main.class";
            RawAssetLoader.getLowLevelClassPathAsset(validClass).dispose();
            return true;
        } catch (Exception ignore) {
            throw new RuntimeException("Minecraft Server Main does not exist.");
        }
    }

    @Override
    public URL getGameJarLocation() {
        if (LoaderConfig.PATCH_PAMPHLET_FILE == null) return null;

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
