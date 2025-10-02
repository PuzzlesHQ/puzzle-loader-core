package dev.puzzleshq.puzzleloader.loader.launch.bootstrap;

import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.launch.PrePiece;
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

    private static void getFlags(String[] args) {
        boolean overrides = false;
        boolean dumpClasses = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            String value = i == args.length - 1 ? "true" : args[i + 1].toLowerCase(Locale.ROOT).toLowerCase(Locale.ROOT);

            switch (arg) {
                case "--allow-class-overrides=true": {
                    overrides = true;
                    break;
                }
                case "--allow-class-overrides=false": {
                    overrides = false;
                    break;
                }
                case "--allow-class-overrides": {
                    if (value.equals("false") || value.equals("true")) {
                        overrides = value.equals("true");
                    } else overrides = true;
                    break;
                }
            }

            switch (arg) {
                case "--dump-transformed-classes=true": {
                    overrides = true;
                    break;
                }
                case "--dump-transformed-classes=false": {
                    overrides = false;
                    break;
                }
                case "--dump-transformed-classes": {
                    if (value.equals("false") || value.equals("true")) {
                        overrides = value.equals("true");
                    } else overrides = true;
                    break;
                }
            }

        }

        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.dumpTransformedClasses", String.valueOf(dumpClasses));
        System.setProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.allowClassOverrides", String.valueOf(overrides));
    }

    public static void loadFlags() {
        PieceClassLoader.overrides = Boolean.parseBoolean(System.getProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.allowClassOverrides"));
        PieceClassLoader.dumpClasses = Boolean.parseBoolean(System.getProperty("dev.puzzleshq.puzzleloader.loader.launch.boostrap.dumpTransformedClasses"));
    }
}
