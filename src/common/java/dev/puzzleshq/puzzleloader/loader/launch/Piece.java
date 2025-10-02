package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.accesswriter.AccessWriters;
import dev.puzzleshq.accesswriter.api.IWriterFormat;
import dev.puzzleshq.mod.ModFormats;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.mod.util.MixinConfig;
import dev.puzzleshq.puzzleloader.loader.LoaderConfig;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.PreLaunchInit;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInit;
import dev.puzzleshq.puzzleloader.loader.patching.PatchLoader;
import dev.puzzleshq.puzzleloader.loader.patching.PatchPage;
import dev.puzzleshq.puzzleloader.loader.patching.PatchPamphlet;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.threading.OffThreadExecutor;
import dev.puzzleshq.puzzleloader.loader.util.*;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
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

    private static final Logger LOGGER = LogManager.getLogger("Puzzle | Piece");

    public static Piece INSTANCE;

    public static void launch(String[] args, EnvType type) {
        Piece piece = new Piece();
        env.set(type);
        piece.privateLaunch(args);
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

    private void privateLaunch(String[] args) {
        LoaderConfig.COMMAND_LINE_ARGUMENTS = args;

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
            LoaderConfig.MIXINS_ENABLED = mixins_enabled.value(options);
            LoaderConfig.DUMP_TRANSFORMED_CLASSES = PieceClassLoader.dumpClasses;
            LoaderConfig.ALLOWS_CLASS_OVERRIDES = PieceClassLoader.overrides;
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
            {
                AtomicReference<String> atomicGameProvider = new AtomicReference<>();
                AtomicReference<URL> gameJar = new AtomicReference<>();
                if (options.has(game_provider)) atomicGameProvider.set(options.valueOf(game_provider));
                URL url = setup(atomicGameProvider, gameJar, true);
                if (gameJar.get() != null) {
                    classLoader = new PieceClassLoader();
                    Thread.currentThread().setContextClassLoader(classLoader);
                    classLoader.addURL(url);

                    for (URL jvmClassPathUrl : ClassPathUtil.getJVMClassPathUrls()) {
                        if (!jvmClassPathUrl.equals(gameJar.get())) {
                            classLoader.addURL(jvmClassPathUrl);
                        }
                    }

                    setup(atomicGameProvider, new AtomicReference<>(), false);
                }
            }

            ModFormats.register(ModFinder::getMod);
            ModFinder.findMods();

            AccessWriters.init(classLoader);
            discoverAccessWriters(ModFinder.getModsArray());
            if (LoaderConfig.USER_TRANSFORMERS_ENABLED)
                TransformerInit.invokeTransformers(classLoader);

            provider.initArgs(args);

            if (LoaderConfig.TRANSFORMERS_ENABLED)
                provider.registerTransformers(classLoader);
            provider.inject(classLoader);

            if (LoaderConfig.MIXINS_ENABLED) {
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

            PreLaunchInit.invoke();
            OffThreadExecutor.start();
            LOGGER.info("Launching {} version {}", provider.getName(), provider.getVisibleVersion());
            main.invoke(null, (Object) providerArgs);
        } catch (Exception e) {
            LOGGER.error("Unable To Launch", e);
            System.exit(1);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private URL setup(AtomicReference<String> game_provider, AtomicReference<URL> gameJar, boolean firstInit) throws Exception {
        ModFinder.crawlModsFolder();

        if (firstInit) {
            if (game_provider.get() != null)
                provider = (IGameProvider) Class.forName(game_provider.get(), true, classLoader).newInstance();
            else {
                for (String builtInProvider : BUILT_IN_PROVIDERS) {
                    provider = (IGameProvider) Class.forName(builtInProvider, true, classLoader).newInstance();
                    if (provider.isValid()) break;
                }
            }
            if (!provider.isValid())
                throw new RuntimeException("Couldn't load any game provider for this particular application.");

            game_provider.set(provider.getClass().getName());

            URL jarURL = provider.getJarLocation();
            if (provider.isBinaryPatchable() && jarURL != null && !LoaderConfig.PATCH_PAMPHLET_FILE.isEmpty()) {
                PatchPamphlet pamphlet = PatchLoader.readPamphlet(new File(LoaderConfig.PATCH_PAMPHLET_FILE));
                if (!pamphlet.isRipped()) {
                    System.out.println("Read Pamphlet(patches) (Name: \"" + pamphlet.getDisplayName() + "\", \"Version\": " + pamphlet.getVersion() + ")");

                    PatchPage patchPage = pamphlet.getClientPatches();
                    if (Piece.getSide() == EnvType.SERVER) patchPage = pamphlet.getServerPatches();
                    if (patchPage != null) {
                        gameJar.set(jarURL);

                        File file = new File("./.puzzle/patched/" + patchPage.getChecksum().substring(0, 2) + "/" + patchPage.getChecksum() + ".jar");
                        if (!file.exists()) {
                            InputStream inputStream = jarURL.openStream();
                            byte[] bytes = JavaUtils.readAllBytes(inputStream);
                            inputStream.close();

                            file.getParentFile().mkdirs();

                            FileOutputStream out = new FileOutputStream(file);
                            patchPage.apply(bytes, out);
                            out.close();
                        }
                        return file.toURI().toURL();
                    }
                }
            }
            return null;
        }
        provider = (IGameProvider) Class.forName(game_provider.get(), true, classLoader).newInstance();

        return null;
    }

    private static void addURL(URL url) {
        classLoader.addURL(url);
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
        Map<String, String> configToMod = new HashMap<>();
        for (IModContainer mod : ModFinder.getModsArray()) {
            for (MixinConfig mixinConfig : mod.getInfo().getMixinConfigs()) {
                mixinConfigs.add(mixinConfig);
                configToMod.put(mixinConfig.path(), "(" + mod.getID() + ")");
            }
        }

        EnvType envType = Piece.getSide();
        mixinConfigs.forEach((e) -> {
            if (Objects.equals(envType.name, e.environment()) || Objects.equals(e.environment(), EnvType.UNKNOWN.name)) {
                Mixins.addConfiguration(e.path());
            }
        });

        for (Config config : Mixins.getConfigs()) {
            config.getConfig().decorate(FabricUtil.KEY_MOD_ID, configToMod.getOrDefault(config.getName(), "(unknown)"));
        }
    }

}