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

        ModFinder.setModFolder(new File("pmods").getAbsoluteFile());
        ModFinder.crawlModsFolder();

        blackboard = new HashMap<>();
    }

    public static EnvType getSide() {
        if (env.get() != null) return env.get();

        try {
            Class.forName("finalforeach.cosmicreach.ClientSingletons");
        } catch (ClassNotFoundException e) {
            env.set(EnvType.SERVER);
        }
        env.set(EnvType.CLIENT);
        return env.get();
    }

    private void launch(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        final OptionSet options = parser.parse(args);
        try {
            OptionSpec<String> provider_option = parser.accepts("gameProvider").withOptionalArg().ofType(String.class);
            OptionSpec<String> modFolder_option = parser.accepts("modFolder").withOptionalArg().ofType(String.class);
            OptionSpec<String> modPaths = parser.accepts("mod-paths").withOptionalArg().ofType(String.class);

//            if (options.has(modPaths)) {
//                String v = modPaths.value(options);
//                if (!v.contains(File.pathSeparator)) {
//                    addFile(new File(v));
//                } else {
//                    String[] jars = modPaths.value(options).split(File.pathSeparator);
//                    for (String jar : jars) addFile(new File(jar));
//                }
//            }

//            if (options.has(modFolder_option))
//                ModFinder.setModFolder(new File(modFolder_option.value(options)).getAbsoluteFile());
//            else
//                ModFinder.setModFolder(new File("pmods").getAbsoluteFile());
//
//            ModFinder.crawlModsFolder();

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

            if (options.has(provider_option))
                provider = (IGameProvider) Class.forName(provider_option.value(options), true, classLoader).newInstance();
            else {
                for (String builtInProvider : BUILT_IN_PROVIDERS) {
                    provider = (IGameProvider) Class.forName(builtInProvider, true, classLoader).newInstance();
                    if (provider.isValid()) break;
                }
            }
            if (!provider.isValid()) throw new RuntimeException("Couldn't load any game provider for this particular application.");

            ModFormats.register(ModFinder::getMod);
            ModFinder.findMods();

            AccessWriters.init(classLoader);
            discoverAccessWriters(ModFinder.getModsArray());

            provider.initArgs(args);

            TransformerInitializer.invokeTransformers(classLoader);
            provider.registerTransformers(classLoader);
            provider.inject(classLoader);

            MixinUtil.start();
            MixinUtil.doInit(new ArrayList<>());
            Piece.setupModMixins();
            MixinUtil.inject();
            MixinUtil.goToPhase(MixinEnvironment.Phase.DEFAULT);

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