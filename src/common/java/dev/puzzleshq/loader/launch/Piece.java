package dev.puzzleshq.loader.launch;

import dev.puzzleshq.loader.mod.entrypoint.PreLaunchInitializer;
import dev.puzzleshq.loader.mod.entrypoint.TransformerInitializer;
import dev.puzzleshq.loader.provider.game.IGameProvider;
import dev.puzzleshq.loader.threading.OffThreadExecutor;
import dev.puzzleshq.loader.util.ClassPathUtil;
import dev.puzzleshq.loader.util.EnvType;
import dev.puzzleshq.loader.util.ModFinder;
import dev.puzzleshq.loader.util.ReflectionUtil;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Piece {
    public static String COSMIC_PROVIDER = "dev.puzzleshq.loader.provider.game.impl.CosmicReachProvider";
    public static String MINECRAFT_PROVIDER = "dev.puzzleshq.loader.provider.game.impl.MinecraftProvider";

    public static IGameProvider provider;

    public static Map<String, Object> blackboard;
    public static PieceClassLoader classLoader;

    static AtomicReference<EnvType> env = new AtomicReference();

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

            classLoader.addClassLoaderExclusion("dev.puzzleshq.loader.");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.loader.mod");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.loader.util");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.loader.launch");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.loader.loading");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.loader.provider");
            classLoader.addClassLoaderExclusion("dev.puzzleshq.loader.transformers");

            if (options.has(provider_option))
                provider = (IGameProvider) Class.forName(provider_option.value(options), true, classLoader).newInstance();
            else {
                try {
                    provider = (IGameProvider) Class.forName(MINECRAFT_PROVIDER, true, classLoader).newInstance();
                } catch (Exception ignore) {}
            }

            if (!provider.isValid()) {
                provider = (IGameProvider) Class.forName(COSMIC_PROVIDER, true, classLoader).newInstance();
            }

            ModFinder.findMods();

            provider.initArgs(args);
            TransformerInitializer.invokeTransformers(classLoader);
            provider.registerTransformers(classLoader);
            MixinBootstrap.init();

            provider.inject(classLoader);
            MixinBootstrap.getPlatform().init();

            String entryPoint = provider.getEntrypoint();
            String ranEntrypoint = entryPoint;
            if (entryPoint.contains("MinecraftApplet")) {
                ranEntrypoint = "dev.puzzleshq.minecraft.launch.MinecraftAppletLauncher";
            }

            Class<?> clazz = Class.forName(ranEntrypoint, false, classLoader);
            MixinEnvironment.init(MixinEnvironment.Phase.DEFAULT);
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

}