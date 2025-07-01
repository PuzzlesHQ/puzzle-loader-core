package dev.puzzleshq.puzzleloader.loader.provider.game.impl;

import com.github.zafarkhaja.semver.Version;
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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ProjectZomboidProvider implements IGameProvider {

    public ProjectZomboidProvider() {
        Piece.provider = this;
    }

    @Override
    public String getId() {
        return "project-zomboid";
    }

    @Override
    public String getName() {
        return "Project Zomboid";
    }

    @Override
    public Version getGameVersion() {
        return Version.parse(getRawVersion());
    }

    @Override
    public String getRawVersion() {
        return "1.0.0";
    }

    @Override
    public String getEntrypoint() {
        String launcher = "/zombie/network/GameServer.class";
        if (LoaderConstants.SIDE == EnvType.SERVER) {
            try {
                RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
                return launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", "");
            } catch (Exception ignore) {
                throw new RuntimeException("Project Zomboid Server Main does not exist.");
            }
        }
        try {
            launcher = "/zombie/gameStates/MainScreenState.class";
            RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
        } catch (Exception e) {
            throw new RuntimeException("Project Zomboid Client Main does not exist.");
        }

        return launcher.replaceFirst("/", "").replaceAll("/", ".").replace(".class", "");
    }

    @Override
    public Collection<String> getArgs() {
        return args;
    }

    @Override
    public void registerTransformers(PieceClassLoader classLoader) {}

    List<String> args;

    @Override
    public void initArgs(String[] args) {
        this.args = new ArrayList<>(Arrays.asList(args));
    }

    @Override
    public void inject(PieceClassLoader classLoader) {

    }

    @Override
    public void addBuiltinMods() {
        ModInfoBuilder projectZomboidModInfo = new ModInfoBuilder();
        {
            projectZomboidModInfo.setDisplayName(getName());
            projectZomboidModInfo.setId(getId());
            projectZomboidModInfo.setDescription("The base game.");
            projectZomboidModInfo.addAuthor("The Indie Stone");
            projectZomboidModInfo.setVersion(getRawVersion());
            projectZomboidModInfo.addMeta("icon", JsonObject.valueOf("pack.png"));
            ModFinder.addModWithContainer(new ModContainer(projectZomboidModInfo.build()));
        }

    }

    @Override
    public String getDefaultNamespace() {
        return "zomboid";
    }

    @Override
    public boolean isValid() {
        try {
            String launcher = "/zombie/network/GameServer.class";
            if (LoaderConstants.SIDE == EnvType.SERVER) {
                try {
                    RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
                    return true;
                } catch (Exception ignore) {
                    throw new RuntimeException("Project Zomboid Server Main does not exist.");
                }
            }
            try {
                launcher = "/zombie/gameStates/MainScreenState.class";
                RawAssetLoader.getLowLevelClassPathAsset(launcher).dispose();
            } catch (Exception e) {
                throw new RuntimeException("Project Zomboid Client Main does not exist.");
            }
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

}
