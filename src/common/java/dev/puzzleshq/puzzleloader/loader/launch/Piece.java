package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.accesswriter.AccessWriters;
import dev.puzzleshq.accesswriter.api.IWriterFormat;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.PreLaunchInitializer;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInitializer;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.threading.OffThreadExecutor;
import dev.puzzleshq.puzzleloader.loader.util.*;
import dev.puzzleshq.mod.ModFormats;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.util.MixinConfig;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Piece {
    private static String[] BUILT_IN_PROVIDERS = {
        "dev.puzzleshq.puzzleloader.loader.provider.game.impl.CosmicReachProvider",
        "dev.puzzleshq.puzzleloader.loader.provider.game.impl.MinecraftProvider",
        "dev.puzzleshq.puzzleloader.loader.provider.game.impl.ProjectZomboidProvider"
    };

    public static IGameProvider provider;

    public static Map<String, Object> blackboard;
    public static PieceClassLoader classLoader;

    static AtomicReference<EnvType> env = new AtomicReference<>();

    private static final Logger LOGGER = LoggerFactory.getLogger("Puzzle | Piece");

    public static Piece INSTANCE;

    public static void launch(String[] args, EnvType type) {
        Piece piece = new Piece();
        env.set(type);
        piece.launch(args);
    }

    private Piece() {
        Piece.INSTANCE = this;

        if (classLoader != null) throw new RuntimeException("MORE THAN ONE PIECE CANNOT EXIST AT THE SAME TIME.");

        classLoader = new PieceClassLoader();
        classLoader.addURL(ClassPathUtil.getJVMClassPathUrls());
        Thread.currentThread().setContextClassLoader(classLoader);

        blackboard = new HashMap<>();
    }

    public static EnvType getSide() {
        return env.get();
    }

    private void launch(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        try {
            OptionSpec<String> game_provider = parser.accepts("game-provider")
                    .withOptionalArg().ofType(String.class);

            OptionSpec<String> mod_folder = parser.accepts("mod-folder")
                    .withOptionalArg().ofType(String.class).defaultsTo(new File("pmods").getAbsolutePath());

            OptionSpec<String> mod_paths = parser.accepts("mod-paths")
                    .withOptionalArg().ofType(String.class);

            OptionSpec<Boolean> do_title_transformer = parser.accepts("do-title-transformer")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            OptionSpec<String> custom_title_format = parser.accepts("custom-title-formatter")
                    .withOptionalArg().ofType(String.class).defaultsTo("Puzzle Loader: %s");

            OptionSpec<Boolean> mixins_enabled = parser.accepts("mixins-enabled")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            final OptionSet options = parser.parse(args);

            LoaderConstants.CLIConfiguration.DO_TITLE_TRANSFORMER = do_title_transformer.value(options);
            LoaderConstants.CLIConfiguration.CUSTOM_TITLE_FORMAT = custom_title_format.value(options);
            LoaderConstants.CLIConfiguration.MIXINS_ENABLED = mixins_enabled.value(options);

            if (options.has(mod_paths)) {
                String v = mod_paths.value(options);
                if (!v.contains(File.pathSeparator)) {
                    addFile(new File(v));
                } else {
                    String[] jars = mod_paths.value(options).split(File.pathSeparator);
                    for (String jar : jars) addFile(new File(jar));
                }
            }

            ModFinder.setModFolder(new File(mod_folder.value(options)).getAbsoluteFile());
            ModFinder.crawlModsFolder();

            /* Support places where the old packages are used */
            classLoader.addClassLoaderExclusion("com.github.puzzle.loader.launch");
            classLoader.addClassLoaderExclusion("com.github.puzzle.core.loader.launch");

            classLoader.addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.mod");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.util");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.launch");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.loading");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.provider");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.transformers");

            if (options.has(game_provider))
                provider = (IGameProvider) Class.forName(game_provider.value(options), true, classLoader).newInstance();
            else {
                for (String builtInProvider : BUILT_IN_PROVIDERS) {
                    provider = (IGameProvider) Class.forName(builtInProvider, true, classLoader).newInstance();
                    if (provider.isValid()) break;
                }
            }
            if (!provider.isValid())
                throw new RuntimeException("Couldn't load any game provider for this particular application.");

            ModFormats.register(ModFinder::getMod);
            ModFinder.findMods();

            AccessWriters.init(classLoader);
            discoverAccessWriters(ModFinder.getModsArray());

            provider.initArgs(args);

            TransformerInitializer.invokeTransformers(classLoader);
            provider.registerTransformers(classLoader);
            provider.inject(classLoader);

            if (LoaderConstants.CLIConfiguration.MIXINS_ENABLED) {
                MixinUtil.start();
                MixinUtil.doInit(new ArrayList<>());
                Piece.setupModMixins();
                MixinUtil.inject();
                MixinUtil.goToPhase(MixinEnvironment.Phase.DEFAULT);
            }

            String entryPoint = provider.getEntrypoint();
            String ranEntrypoint = entryPoint;
            if (entryPoint.contains("MinecraftApplet")) {
                ranEntrypoint = "dev.puzzleshq.puzzleloader.minecraft.launch.MinecraftAppletLauncher";
            }

            Class<?> clazz = Class.forName(ranEntrypoint, false, classLoader);
            String[] providerArgs = provider.getArgs().toArray(new String[0]);

            Method main = ReflectionUtil.getMethod(clazz, "main", String[].class);

            PreLaunchInitializer.invoke();
            OffThreadExecutor.start();
            LOGGER.info("Launching {} version {}", provider.getName(), provider.getRawVersion());
            main.invoke(null, (Object) providerArgs);
        } catch (Exception e) {
            LOGGER.error("Unable To Launch", e);
            System.exit(1);
        }
    }

    private void addFile(File file) {
        try {
            classLoader.addURL(file.getAbsoluteFile().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void discoverAccessWriters(List<IModContainer> modsArray) {
        for (IModContainer container : modsArray) {
            ModInfo info = container.getInfo();

            String[] transformers = info.getAccessTransformers();

            for (String transformerPath : transformers) {
                RawAssetLoader.RawFileHandle handle = RawAssetLoader.getLowLevelClassPathAsset(transformerPath);
                if (handle == null) {
                    LOGGER.warn("AccessWriter at \"{}\" does not exist, please remove from your puzzle.mod.json manifest", transformerPath);
                    continue;
                }

                IWriterFormat format = AccessWriters.getFormat(transformerPath);
                if (format == null)
                    throw new RuntimeException("Unsupported AccessWriter format found in file \"" + transformerPath + "\", please remove this file or fix the format or the crash will persist.");

                try {
                    AccessWriters.MERGED.add(format.parse(handle.getString()));
                } catch (Exception e) {
                    LOGGER.error("Error on File: {}", handle.getFile(), e);
                }
            }
        }
    }

    public static void setupModMixins() {
        List<MixinConfig> mixinConfigs = new ArrayList<>();
        for (IModContainer mod : ModFinder.getModsArray()) {
            if (mod.getInfo().getMixinConfigs().length != 0)
                mixinConfigs.addAll(List.of(mod.getInfo().getMixinConfigs()));
        }

        EnvType envType = LoaderConstants.SIDE;
        mixinConfigs.forEach((e) -> {
            if (Objects.equals(envType.name, e.environment()) || Objects.equals(e.environment(), EnvType.UNKNOWN.name)) {
                Mixins.addConfiguration(e.path());
            }
        });
    }

}