package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.accesswriter.AccessWriters;
import dev.puzzleshq.accesswriter.api.IWriterFormat;
import dev.puzzleshq.mod.ModFormats;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.puzzleloader.loader.LoaderConfig;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.PreLaunchInit;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInit;
import dev.puzzleshq.puzzleloader.loader.patching.PatchLoader;
import dev.puzzleshq.puzzleloader.loader.patching.PatchPage;
import dev.puzzleshq.puzzleloader.loader.patching.PatchPamphlet;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.provider.game.IPatchableGameProvider;
import dev.puzzleshq.puzzleloader.loader.threading.OffThreadExecutor;
import dev.puzzleshq.puzzleloader.loader.util.*;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code Piece} class is the main launcher and core manager for the Puzzle Loader.
 * <p>
 * It handles environment setup, argument parsing, mod discovery, class loading,
 * and the initialization of the selected {@link IGameProvider}.
 * This class is designed to exist as a single instance during runtime.
 */
public class Piece {

    /** The active game provider instance that manages the game runtime. */
    public static IGameProvider gameProvider;

    /** A shared key-value store used for inter-component communication. */
    public static Map<String, Object> blackboard;

    /** The custom class loader responsible for loading mods and game classes. */
    public static PieceClassLoader classLoader;

    /** The current environment type (client, server, or other). */
    static AtomicReference<EnvType> env = new AtomicReference<>();

    /** The logger used by the Piece system for output and diagnostics. */
    private static final Logger LOGGER = LogManager.getLogger("Puzzle | Piece");

    /** The singleton instance of {@code Piece}. */
    public static Piece INSTANCE;

    /**
     * Launches the Puzzle Loader with the provided arguments and environment type.
     *
     * @param args the command-line arguments passed to the game
     * @param type the environment type (client or server)
     */
    public static void launch(String[] args, EnvType type) {
        Piece piece = new Piece();
        env.set(type);
        piece.privateLaunch(args);
    }

    /**
     * Constructs a new {@code Piece} instance.
     * <p>
     * Ensures that only one instance exists at a time, initializes the
     * blackboard, and sets up the {@link PieceClassLoader}.
     *
     * @throws RuntimeException if another instance already exists
     */
    private Piece() {
        Piece.INSTANCE = this;

        if (classLoader != null) throw new RuntimeException("MORE THAN ONE PIECE CANNOT EXIST AT THE SAME TIME.");

        blackboard = new HashMap<>();
        classLoader = new PieceClassLoader();

        classLoader.addURL(ClassPathUtil.getJVMClassPathUrls());
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    /**
     * Returns the current environment type (client, server, etc.).
     *
     * @return the active {@link EnvType}
     */
    public static EnvType getSide() {
        return env.get();
    }

    /**
     * Performs the internal launch sequence for Puzzle Loader.
     * <p>
     * Parses arguments, initializes mods, applies patches and transformers,
     * and executes the game provider’s main entry point.
     *
     * @param args the command-line arguments for the game
     */
    private void privateLaunch(String[] args) {
        LoaderConfig.COMMAND_LINE_ARGUMENTS = args;

        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        try {
            OptionSpec<String> mod_folder = parser.accepts("mod-folder")
                    .withOptionalArg().ofType(String.class).defaultsTo(new File("pmods").getAbsolutePath());

            OptionSpec<String> mod_paths = parser.accepts("mod-paths")
                    .withOptionalArg().ofType(String.class);

            OptionSpec<Boolean> do_title_transformer = parser.accepts("do-title-transformer")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            OptionSpec<String> custom_title_format = parser.accepts("custom-title-formatter")
                    .withOptionalArg().ofType(String.class).defaultsTo("Puzzle Loader: %s");

            OptionSpec<Boolean> transformers_enabled = parser.accepts("transformers-enabled")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            OptionSpec<Boolean> user_transformers_enabled = parser.accepts("user-transformers-enabled")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            OptionSpec<String> patch_file = parser.accepts("pamphlet")
                    .withOptionalArg().ofType(String.class).defaultsTo("");

            final OptionSet options = parser.parse(args);

            LoaderConfig.PATCH_PAMPHLET_FILE = patch_file.value(options);

            LoaderConfig.DO_TITLE_TRANSFORMER = do_title_transformer.value(options);
            LoaderConfig.CUSTOM_TITLE_FORMAT = custom_title_format.value(options);

            LoaderConfig.TRANSFORMERS_ENABLED = transformers_enabled.value(options);
            LoaderConfig.USER_TRANSFORMERS_ENABLED = user_transformers_enabled.value(options) && transformers_enabled.value(options);

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

            gameProvider = IGameProvider.findValidProvider();

            // allow the game provider to control mixins, class dumping
            String mixinProperty = System.getProperty("puzzle.core.mixin.enabled");
            LoaderConfig.MIXINS_ENABLED = mixinProperty == null || mixinProperty.isEmpty() || "true".equals(mixinProperty);

            PieceClassLoader.loadSystemProperties();

            if (!"true".equals(System.getProperty("puzzle.core.disable-patching"))) {
                IPatchableGameProvider.patchAndReload(gameProvider);
            }

            if (!"true".equals(System.getProperty("puzzle.core.disable-mod-search"))) {
                ModFinder.crawlModsFolder();
                ModFormats.register(ModFinder::getMod);
                ModFinder.findMods();
            }

            if (!"true".equals(System.getProperty("puzzle.core.disable-all-transform"))) {
                AccessWriters.init(classLoader);
                discoverAccessWriters(ModFinder.getModsArray());
                if (LoaderConfig.USER_TRANSFORMERS_ENABLED)
                    TransformerInit.invokeTransformers(classLoader);
            }

            gameProvider.initArgs(args);

            if (!"true".equals(System.getProperty("puzzle.core.disable-all-transform"))) {
                if (LoaderConfig.TRANSFORMERS_ENABLED)
                    gameProvider.registerTransformers(classLoader);
                gameProvider.inject(classLoader);

                if (LoaderConfig.MIXINS_ENABLED) {
                    gameProvider.startMixins();
                }
            }

            String entryPoint = gameProvider.getEntrypoint();
            String ranEntrypoint = entryPoint;
            if (entryPoint.contains("MinecraftApplet")) {
                ranEntrypoint = "dev.puzzleshq.puzzleloader.minecraft.launch.MinecraftAppletLauncher";
            }

            Class<?> clazz = Class.forName(ranEntrypoint, false, classLoader);
            String[] providerArgs = gameProvider.getArgs().toArray(new String[0]);

            Method main = ReflectionUtil.getMethod(clazz, "main", String[].class);

            Class<?> entrypointClazz = Class.forName(
                    "dev.puzzleshq.puzzleloader.loader.mod.entrypoint.PreLaunchInit",
                    true,
                    classLoader
            );

            Method invoker = entrypointClazz.getDeclaredMethod("onPreLaunch");

            for (PuzzleEntrypointUtil.Entrypoint<?> preLaunch : PuzzleEntrypointUtil.getEntrypoints("preLaunch", entrypointClazz)) {
                try {
                    invoker.invoke(preLaunch.createInstance());
                } catch (Exception ignore) {}
            }
            OffThreadExecutor.start();
            LOGGER.info("Launching {} version {}", gameProvider.getName(), gameProvider.getVisibleVersion());
            main.invoke(null, (Object) providerArgs);
        } catch (Exception e) {
            LOGGER.error("Unable To Launch", e);
            System.exit(1);
        }
    }

    /**
     * Adds a URL to the {@link PieceClassLoader}.
     * <p>
     * This method is used internally by Puzzle Paradox.
     *
     * @param url the URL to be added to the classpath
     */
    private static void addURL(URL url) {
        classLoader.addURL(url);
    }

    /**
     * Adds a file to the {@link PieceClassLoader} as a classpath entry.
     *
     * @param file the file to add
     * @throws RuntimeException if the file’s URL is malformed
     */
    private void addFile(File file) {
        try {
            classLoader.addURL(file.getAbsoluteFile().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Discovers and loads access transformers (AccessWriters) from the provided mods.
     * <p>
     * Each transformer file is validated, parsed, and merged into the runtime access writer set.
     *
     * @param modsArray the list of mod containers to inspect
     */
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

}
