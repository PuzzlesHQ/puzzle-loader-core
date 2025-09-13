package dev.puzzleshq.puzzleloader.loader.launch.bootstrap;

import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.launch.PrePiece;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public class BootstrapPiece {

    private static Logger LOGGER;
    public static String ENV;

    public static void launch(String[] args, String name) {
        getFlags(args);
        loadFlags();

        ENV = name;

        PrePiece.launch(args, name);
    }

    public static void loadWithoutBootstrap(String[] args, String name) {
        PrePiece.launch(args, name);
    }

    private static void getFlags(String[] args) {
        boolean overrides = false;
        boolean dumpClasses = false;
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

        }

        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.dumpTransformedClasses", String.valueOf(dumpClasses));
        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.allowClassOverrides", String.valueOf(overrides));
    }

    public static Logger getLogger() {
        if (LOGGER == null)
            LOGGER = LogManager.getLogger("Puzzle | Emergency Piece");
        return LOGGER;
    }

    public static void loadFlags() {
        boolean dumpClasses = Boolean.parseBoolean(System.getProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.dumpTransformedClasses"));
        boolean overrides = Boolean.parseBoolean(System.getProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.allowClassOverrides"));

        PieceClassLoader.overrides = overrides;
        PieceClassLoader.dumpClasses = dumpClasses;
    }
}
