package dev.puzzleshq.puzzleloader.loader.launch.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

public class BootstrapPiece {

    private static Logger LOGGER;

    public static BootstrapClassLoader boostrapClassloader;

    public static void launch(String[] args, String name) {
        boostrapClassloader = new BootstrapClassLoader(new URL[0], BootstrapPiece.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(boostrapClassloader);
        testArgs(args);

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
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            String value = i == args.length - 1 ? "true" : args[i + 1].toLowerCase(Locale.ROOT).toLowerCase(Locale.ROOT);

            switch (arg) {
                case "--allow-class-overrides=true" -> overrides = true;
                case "--allow-class-overrides=false" -> overrides = false;
                case "--allow-class-overrides" -> {
                    if (value.equals("true")) overrides = true;
                    else overrides = !value.equals("false");
                }
            }

            switch (arg) {
                case "--dump-transformed-classes=true" -> dumpClasses = true;
                case "--dump-transformed-classes=false" -> dumpClasses = false;
                case "--dump-transformed-classes" -> {
                    if (value.equals("true")) dumpClasses = true;
                    else dumpClasses = !value.equals("false");
                }
            }
        }

        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.dumpTransformedClasses", String.valueOf(dumpClasses));
        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.allowClassOverrides", String.valueOf(overrides));

        BootstrapClassLoader.usesOverrides(overrides);
        BootstrapClassLoader.dumps(dumpClasses);
    }

    public static Logger getLogger() {
        if (LOGGER == null)
            LOGGER = LoggerFactory.getLogger("Puzzle | Emergency Piece");
        return LOGGER;
    }

}
