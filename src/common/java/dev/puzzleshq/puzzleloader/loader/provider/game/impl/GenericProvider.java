package dev.puzzleshq.puzzleloader.loader.provider.game.impl;

import com.github.villadora.semver.Version;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GenericProvider implements IGameProvider {
    private static final Version version = Version.valueOf("0.0.0");
    private final List<String> args = new ArrayList<>();
    private String mainClass;

    @Override
    public String getId() {
        return "generic-process";
    }

    @Override
    public String getName() {
        return "Generic Process";
    }

    @Override
    public Version getGameVersion() {
        return Version.valueOf("0.0.0");
    }

    @Override
    public String getRawVersion() {
        return "0.0.0";
    }

    @Override
    public String getEntrypoint() {
        return mainClass;
    }

    @Override
    public Collection<String> getArgs() {
        return args;
    }

    @Override
    public void initArgs(String[] args) {
        Piece.LOGGER.warn("Couldn't find specific game provider, defaulting to \"{}\"", this.getClass().getSimpleName());

        OptionParser optionparser = new OptionParser();
        optionparser.allowsUnrecognizedOptions();
        OptionSpec<String> mainClassSpec = optionparser.accepts("mainClass").withRequiredArg().required();
        OptionSet optionSet = optionparser.parse(args);

        this.mainClass = mainClassSpec.value(optionSet);

        this.args.addAll(Arrays.asList(args));
    }

    @Override
    public void addBuiltinMods() {

    }

    @Override
    public String getDefaultNamespace() {
        return "generic";
    }

    @Override
    public boolean isValid() {
        RawAssetLoader.RawFileHandle handle = RawAssetLoader.getLowLevelClassPathAssetErrors(mainClass, false);
        return handle != null;
    }
}
