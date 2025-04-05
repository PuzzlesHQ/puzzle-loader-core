package com.github.puzzle.loader.launch;

import com.github.puzzle.loader.provider.game.IGameProvider;
import com.github.puzzle.loader.util.EnvType;
import com.github.puzzle.loader.util.MixinUtil;
import com.github.puzzle.loader.util.ModLocator;
import com.github.puzzle.loader.util.Reflection;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.util.asm.ASM;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Piece {
    public static String COSMIC_PROVIDER = "com.github.puzzle.loader.provider.game.impl.CosmicReachProvider";
    public static String MINECRAFT_PROVIDER = "com.github.puzzle.loader.provider.game.impl.MinecraftProvider";

    public static IGameProvider provider;

    public static Map<String, Object> blackboard;
    public static PuzzleClassLoader classLoader;

    static AtomicReference<EnvType> env = new AtomicReference();

    public static final Logger LOGGER = LogManager.getLogger("Puzzle | Loader");

    public static void launch(String[] args, EnvType type) {
        Piece piece = new Piece();
        env.set(type);
        piece.launch(args);
    }

    List<URL> classPath = new ArrayList();

    private Piece() {
        if (classLoader != null) throw new RuntimeException("MORE THAN ONE PIECE CANNOT EXIST AT THE SAME TIME.");

        classPath.addAll(ModLocator.getUrlsOnClasspath());
        ModLocator.crawlModsFolder(classPath);

        classLoader = new PuzzleClassLoader(classPath);
        blackboard = new HashMap();
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    public static void main(String[] args) {
        Piece piece = new Piece();
        piece.launch(args);
    }

    public static EnvType getSide() {
        if (env.get() != null) return env.get();

        try {
            Class.forName("finalforeach.cosmicreach.ClientSingletons");
        } catch (ClassNotFoundException e) {
            env.set(EnvType.SERVER);
        }
        env.set(EnvType.CLIENT);;
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

            classLoader = new PuzzleClassLoader(classPath);
            Thread.currentThread().setContextClassLoader(classLoader);

            if (options.has(modPaths)) {
                String v = modPaths.value(options);
                if (!v.contains(File.pathSeparator)) {
                    addFile(new File(v));
                } else {
                    String[] jars = modPaths.value(options).split(File.pathSeparator);
                    for (String jar : jars) addFile(new File(jar));
                }
            }

            if (options.has(modFolder_option)) ModLocator.setModFolder(new File(modFolder_option.value(options)));
            classLoader.addClassLoaderExclusion("com.github.puzzle.loader.launch");
            classLoader.addClassLoaderExclusion("com.github.puzzle.loader.loading");
            classLoader.addClassLoaderExclusion("com.github.puzzle.loader.provider");
            classLoader.addClassLoaderExclusion("com.github.puzzle.loader.mod");
            classLoader.addClassLoaderExclusion("com.github.puzzle.loader.util");
            classLoader.addClassLoaderExclusion("com.github.puzzle.loader.transformers");

            classLoader.addClassLoaderExclusion("com.github.puzzle.core.loader.launch");
            classLoader.addClassLoaderExclusion("com.github.puzzle.core.loader.loading");
            classLoader.addClassLoaderExclusion("com.github.puzzle.core.loader.provider");
            classLoader.addClassLoaderExclusion("com.github.puzzle.core.loader.mod");
            classLoader.addClassLoaderExclusion("com.github.puzzle.core.loader.util");

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

            provider.initArgs(args);
            provider.registerTransformers(classLoader);
            if (MixinUtil.WAS_STARTED) MixinUtil.doInit(new String[0]);
            provider.inject(classLoader);

            String entryPoint = provider.getEntrypoint();
            String ranEntrypoint = entryPoint;
            if (entryPoint.contains("MinecraftApplet")) {
                ranEntrypoint = "net.minecraft.launch.MinecraftAppletLauncher";
            }

            Class<?> clazz = Class.forName(ranEntrypoint, false, classLoader);

            String[] providerArgs = provider.getArgs().toArray(new String[0]);
            Method main = Reflection.getMethod(clazz,"main", String[].class);
            LOGGER.info("Launching {} version {}", provider.getName(), provider.getRawVersion());
            main.invoke(null, (Object) providerArgs);
        } catch (Exception e) {
            LOGGER.error("Unable To Launch", e);
            System.exit(1);
        }
    }

    private void addFile(File f) throws MalformedURLException {
        if (!f.exists()) return;

        if (f.getName().endsWith(".jar")) {
            classLoader.addURL(f.toURL());
            return;
        }

        if (f.isDirectory()) {
            for (File fc : f.listFiles()) {
                addFile(fc);
            }
        }
    }
}