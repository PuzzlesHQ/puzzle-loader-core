package dev.puzzleshq.puzzleloader.loader.launch.bootstrap;

import dev.puzzleshq.puzzleloader.loader.launch.PrePiece;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Locale;

public class BootstrapPiece {

    private static Logger LOGGER;

    public static BootstrapClassLoader boostrapClassloader;

    public static void launch(String[] args, String name) {
        testArgs(args);
        boolean skipBoostrap = loadFlags();

        if (skipBoostrap) loadWithoutBootstrap(args, name);
        else loadBootstrap(args, name);
    }

    public static void loadWithoutBootstrap(String[] args, String name) {
        PrePiece.launch(args, name);
    }

    public static void loadBootstrap(String[] args, String name) {
        boostrapClassloader = new BootstrapClassLoader(new URL[0], BootstrapPiece.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(boostrapClassloader);

        try {
            Class.forName("dev.puzzleshq.puzzleloader.loader.launch.PrePiece", false, boostrapClassloader)
                    .getMethod("launch", String[].class, String.class)
                    .invoke(null, args, name);
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

        BootstrapClassLoader.usesOverrides(overrides);
        BootstrapClassLoader.dumps(dumpClasses);

        return skipBootstrap;
    }
}
