package dev.puzzleshq.puzzleloader.loader.mixin;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.moulberry.mixinconstraints.MixinConstraints;
import com.moulberry.mixinconstraints.mixin.MixinConstraintsBootstrap;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.util.MixinConfig;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.MixinUtil;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;

import java.lang.reflect.Field;
import java.util.*;

public class MixinController {

    public static void initializeMixins() {
        MixinBootstrap.init();
        MixinExtrasBootstrap.init();
        MixinSquaredBootstrap.init();
        MixinSquaredApiImplLoader.load();
    }

    public static Map<String, String> registerModMixins() {
        List<MixinConfig> mixinConfigs = new ArrayList<>();
        Map<String, String> configToMod = new HashMap<>();
        for (IModContainer mod : ModFinder.getModsArray()) {
            for (MixinConfig mixinConfig : mod.getInfo().getMixinConfigs()) {
                mixinConfigs.add(mixinConfig);
                configToMod.put(mixinConfig.path(), "(" + mod.getID() + ")");
            }
        }

        EnvType envType = Piece.getSide();
        mixinConfigs.forEach((e) -> {
            if (Objects.equals(envType.name, e.environment()) || Objects.equals(e.environment(), EnvType.UNKNOWN.name)) {
                Mixins.addConfiguration(e.path());
            }
        });

        MixinSquaredBootstrap.reOrderExtensions();
        return configToMod;
    }

    public static void applyMixinDecorations(Map<String, String> configToMod) {
        for (Config config : Mixins.getConfigs()) {
            IMixinConfig mixinConfig = config.getConfig();
            mixinConfig.decorate(FabricUtil.KEY_MOD_ID, configToMod.getOrDefault(config.getName(), "(unknown)"));
            MixinConstraintsBootstrap.init(mixinConfig.getMixinPackage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void applyMixinConstraints() {
        Class<?> mixinConfigClass;
        Field mixinClassesFieldA;
        Field mixinClassesFieldB;
        Field mixinClassesFieldC;

        try {
            mixinConfigClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
            mixinClassesFieldA = mixinConfigClass.getDeclaredField("mixinClasses");
            mixinClassesFieldA.setAccessible(true);
            mixinClassesFieldB = mixinConfigClass.getDeclaredField("mixinClassesClient");
            mixinClassesFieldB.setAccessible(true);
            mixinClassesFieldC = mixinConfigClass.getDeclaredField("mixinClassesServer");
            mixinClassesFieldC.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        for (Config config : Mixins.getConfigs()) {
            IMixinConfig mixinConfig = config.getConfig();
            if (!mixinConfigClass.isAssignableFrom(mixinConfigClass)) continue;

            try {
                List<String> mixinClassesA = (List<String>) mixinClassesFieldA.get(mixinConfig);
                List<String> mixinClassesB = (List<String>) mixinClassesFieldB.get(mixinConfig);
                List<String> mixinClassesC = (List<String>) mixinClassesFieldC.get(mixinConfig);

                constrainMixinClass(mixinConfig.getMixinPackage(), mixinClassesA);
                constrainMixinClass(mixinConfig.getMixinPackage(), mixinClassesB);
                constrainMixinClass(mixinConfig.getMixinPackage(), mixinClassesC);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void constrainMixinClass(String mixinPackage, List<String> mixinClassesA) {
        if (mixinClassesA == null) return;
        List<String> toRemove = new ArrayList<>();
        for (String s : mixinClassesA) {
            boolean shouldApply = MixinConstraints.shouldApplyMixin(mixinPackage + "." + s);
            if (!shouldApply) toRemove.add(s);
        }
        for (String s : toRemove) {
            mixinClassesA.remove(s);
        }
    }

    public static void injectMixins() {
        MixinUtil.inject();
        MixinUtil.goToPhase(MixinEnvironment.Phase.DEFAULT);
    }
}
