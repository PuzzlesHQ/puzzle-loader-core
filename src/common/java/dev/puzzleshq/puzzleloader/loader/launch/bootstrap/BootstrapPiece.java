package dev.puzzleshq.puzzleloader.loader.launch.bootstrap;

import dev.puzzleshq.puzzleloader.loader.launch.PrePiece;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.FlexGlobalCLSettings;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.impl.FlexClassLoader;
import dev.puzzleshq.puzzleloader.loader.transformers.MixinProxyTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Locale;

public class BootstrapPiece {

    private static Logger LOGGER;

    public static FlexClassLoader boostrapClassloader;
    public static FlexClassLoader generalClassloader;
    public static String ENV;

    public static void launch(String[] args, String envType) {
        ENV = envType;

        testArgs(args);
        boolean skipBoostrap = loadFlags();

        if (skipBoostrap) loadWithoutBootstrap(args, envType);
        else loadBootstrap(args, envType);
    }

    public static void loadWithoutBootstrap(String[] args, String envType) {
        PrePiece.launch(args, envType);
    }

    public static void loadBootstrap(String[] args, String envType) {
        boostrapClassloader = new FlexClassLoader("Bootstrap-FlexClassLoader", new URL[0], BootstrapPiece.class.getClassLoader());
        boostrapClassloader.registerTransformer(new IClassTransformer() {
            @Override
            public byte[] transform(String name, String fileName, byte[] bytes) {
                if (!name.equals("org.spongepowered.asm.mixin.transformer.MixinTransformer")) {
                    return bytes;
                }
                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(0);

                reader.accept(new MixinProxyTransformer(writer), 0);
                return writer.toByteArray();
            }
        });

        Thread.currentThread().setContextClassLoader(boostrapClassloader);

        try {
            Class.forName("dev.puzzleshq.puzzleloader.loader.launch.PrePiece", false, boostrapClassloader)
                    .getMethod("launch", String[].class, String.class)
                    .invoke(null, args, envType);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            getLogger().error("PrePiece seemed to have crashed BootstrapPiece, please contact a puzzle developer in the PuzzleHQ, https://discord.com/invite/XeVud4RC9U", e);
            System.exit(-14);
        }
    }

    private static void testArgs(String[] args) {
        boolean overrides = false;
        boolean dumpClasses = false;
        boolean skipBoostrap = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            String value = i == args.length - 1 ? "true" : args[i + 1].toLowerCase(Locale.ROOT).toLowerCase(Locale.ROOT);

            switch (arg) {
                case "--allow-class-overrides=true" -> overrides = true;
                case "--allow-class-overrides=false" -> overrides = false;
                case "--allow-class-overrides" -> {
                    if (value.equals("false") || value.equals("true")) {
                        overrides = value.equals("true");
                    } else overrides = true;
                }
            }

            switch (arg) {
                case "--dump-transformed-classes=true" -> dumpClasses = true;
                case "--dump-transformed-classes=false" -> dumpClasses = false;
                case "--dump-transformed-classes" -> {
                    if (value.equals("false") || value.equals("true")) {
                        overrides = value.equals("true");
                    } else overrides = true;
                }
            }

            switch (arg) {
                case "--skip-boostrap-classloader=true" -> skipBoostrap = true;
                case "--skip-boostrap-classloader=false" -> skipBoostrap = false;
                case "--skip-boostrap-classloader" -> {
                    if (value.equals("false") || value.equals("true")) {
                        overrides = value.equals("true");
                    } else overrides = true;
                }
            }
        }

        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.dumpTransformedClasses", String.valueOf(dumpClasses));
        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.allowClassOverrides", String.valueOf(overrides));
        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.skipBootstrapClassloader", String.valueOf(skipBoostrap));
    }

    public static Logger getLogger() {
        if (LOGGER == null)
            LOGGER = LoggerFactory.getLogger("Puzzle | Emergency Piece");
        return LOGGER;
    }

    public static boolean loadFlags() {
        boolean dumpClasses = Boolean.parseBoolean(System.getProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.dumpTransformedClasses"));
        boolean overrides = Boolean.parseBoolean(System.getProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.allowClassOverrides"));
        boolean skipBootstrap = Boolean.parseBoolean(System.getProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.skipBootstrapClassloader"));

        FlexGlobalCLSettings.ALLOW_CLASS_OVERRIDING.set(overrides);
        FlexGlobalCLSettings.DUMP_TRANSFORMED_CLASSES.set(dumpClasses);

        return skipBootstrap;
    }
}
