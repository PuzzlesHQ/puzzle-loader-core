package dev.puzzleshq.puzzleloader.loader.util;

import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public class MixinUtil {

    final static String MIXIN_START = "start";
    final static String MIXIN_DO_INIT = "doInit";
    final static String MIXIN_INJECT = "inject";
    final static String MIXIN_GOTO_PHASE = "gotoPhase";

    public static void start() {
        try {
            ReflectionUtil.getMethod(MixinBootstrap.class, MIXIN_START).invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void goToPhase(MixinEnvironment.Phase phase) {
        try {
            ReflectionUtil.getMethod(MixinEnvironment.class, MIXIN_GOTO_PHASE, MixinEnvironment.Phase.class).invoke(null, phase);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void doInit(String[] args) {
        try {
            ReflectionUtil.getMethod(MixinBootstrap.class, MIXIN_DO_INIT, CommandLineOptions.class).invoke(null, CommandLineOptions.of(Arrays.asList(args)));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void doInit(List<String> args) {
        try {
            ReflectionUtil.getMethod(MixinBootstrap.class, MIXIN_DO_INIT, CommandLineOptions.class).invoke(null, CommandLineOptions.of(args));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void doInit(CommandLineOptions args) {
        try {
            ReflectionUtil.getMethod(MixinBootstrap.class, MIXIN_DO_INIT, CommandLineOptions.class).invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void inject() {
        try {
            ReflectionUtil.getMethod(MixinBootstrap.class, MIXIN_INJECT).invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
