package dev.puzzleshq.loader.util;

import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public class MixinUtil {

    public static boolean WAS_STARTED = false;

    final static String MIXIN_START = "start";
    final static String MIXIN_DO_INIT = "doInit";
    final static String MIXIN_INJECT = "inject";
    final static String MIXIN_GOTO_PHASE = "gotoPhase";

    public static void start() {
        WAS_STARTED = true;
        Reflection.runStaticMethod(Reflection.getMethod(MixinBootstrap.class, MIXIN_START));
    }

    public static void goToPhase(MixinEnvironment.Phase phase) {
        Reflection.runStaticMethod(Reflection.getMethod(MixinEnvironment.class, MIXIN_GOTO_PHASE, MixinEnvironment.Phase.class), phase);
    }

    public static void doInit(String[] args) {
        Reflection.runStaticMethod(Reflection.getMethod(MixinBootstrap.class, MIXIN_DO_INIT, CommandLineOptions.class), CommandLineOptions.of(Arrays.asList(args)));
    }

    public static void doInit(List<String> args) {
        Reflection.runStaticMethod(Reflection.getMethod(MixinBootstrap.class, MIXIN_DO_INIT, CommandLineOptions.class), CommandLineOptions.of(args));
    }

    public static void doInit(CommandLineOptions args) {
        Reflection.runStaticMethod(Reflection.getMethod(MixinBootstrap.class, MIXIN_DO_INIT, CommandLineOptions.class), args);
    }

    public static void inject() {
        Reflection.runStaticMethod(Reflection.getMethod(MixinBootstrap.class, MIXIN_INJECT));
    }

}
