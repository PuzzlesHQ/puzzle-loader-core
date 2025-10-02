package dev.puzzleshq.puzzleloader.loader;

import dev.puzzleshq.annotation.documentation.Note;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import dev.puzzleshq.puzzleloader.loader.util.ReflectionUtil;
import dev.puzzleshq.puzzleloader.loader.util.ResourceLocation;

public class LoaderConstants {

    public static final EnvType SIDE = Piece.getSide();

    public static final String PUZZLE_CORE_VERSION = getPuzzleCoreVersion();

    public static final String FULL_MIXIN_VERSION = getMixinVersion();
    public static final String SHORT_MIXIN_VERSION = FULL_MIXIN_VERSION.replaceAll("\\+.*", "");

    private static String getPuzzleCoreVersion() {
        RawAssetLoader.RawFileHandle handle = RawAssetLoader.getClassPathAsset(ResourceLocation.of("puzzle-loader-core:core-version.txt"));
        String version = handle.getString();
        handle.dispose();
        if (!version.contains(".")) {
            return "0.0.0-alpha";
        } else return version;
    }

    private static String getMixinVersion() {
        RawAssetLoader.RawFileHandle handle = RawAssetLoader.getClassPathAsset(ResourceLocation.of("puzzle-loader-core:mixin-version.txt"));
        String version = handle.getString();
        handle.dispose();
        if (!version.contains(".")) {
            return "0.0.0-alpha";
        } else return version;
    }

    @Note("Only works for gdx so far, other game impls need to be added.")
    public static boolean shouldClose() {
        try {
            Class<?> gdxClass = Class.forName("com.badlogic.gdx");
            Object app = ReflectionUtil.getField(gdxClass, "app").get(null);
            if (app == null) {
                System.out.println("Constants.shouldClose() not implemented for this game.");
                return false;
            }

            return (boolean) ReflectionUtil.getField(app, "running").get(app);
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            return false;
        }
    }
}
