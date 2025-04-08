package dev.puzzleshq.loader;

import dev.puzzleshq.loader.launch.Piece;
import dev.puzzleshq.loader.util.EnvType;
import dev.puzzleshq.loader.util.RawAssetLoader;
import dev.puzzleshq.loader.util.Reflection;
import dev.puzzleshq.loader.util.ResourceLocation;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.lang.invoke.MethodHandles;

public class Constants {

    public static final IEventBus EVENT_BUS = new EventBus();

    public static final EnvType SIDE = Piece.getSide();
    public static final MixinEnvironment.CompatibilityLevel MIXIN_COMPATIBILITY_LEVEL = MixinEnvironment.CompatibilityLevel.JAVA_8;

    public static final String PUZZLE_CORE_VERSION = getPuzzleCoreVersion();
    public static final boolean IS_CORE_DEV = getPuzzleCoreVersion().equals("69.69.69");

    private static String getPuzzleCoreVersion() {
        RawAssetLoader.RawFileHandle handle = RawAssetLoader.getClassPathAsset(ResourceLocation.of("puzzle-loader:core-version.txt"));
        String version = handle.getString();
        handle.dispose();
        if (!version.contains(".")) {
            return "69.69.69";
        } else return version;
    }

    static {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        alphabet += alphabet.toUpperCase();
        String[] abcs = alphabet.split("");
        for (String s : abcs) {
            EVENT_BUS.registerLambdaFactory(s, (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
        }
    }

    public static boolean shouldClose() {
        try {
            Class<?> gdxClass = Class.forName("com.badlogic.gdx");
            Object app = Reflection.getFieldContents(gdxClass, "app");
            if (app == null) return false;

            return Reflection.getFieldContents(app, "running");
        } catch (ClassNotFoundException ignored) {}

        return false;
    }
}
