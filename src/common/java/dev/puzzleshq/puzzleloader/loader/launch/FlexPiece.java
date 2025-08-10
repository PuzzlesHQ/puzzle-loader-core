package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.accesswriter.AccessWriters;
import dev.puzzleshq.mod.ModFormats;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.api.format.impl.ModFormatV2;
import dev.puzzleshq.mod.api.format.impl.ModFormatV3;
import dev.puzzleshq.mod.util.MixinConfig;
import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapPiece;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.PreLaunchInitializer;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInitializer;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.FlexClassRestriction;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.impl.FlexClassLoader;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.provider.mixin.transformers.BetterProxy;
import dev.puzzleshq.puzzleloader.loader.threading.OffThreadExecutor;
import dev.puzzleshq.puzzleloader.loader.util.*;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class FlexPiece {

    private static final String[] BUILT_IN_PROVIDERS = {
            "dev.puzzleshq.puzzleloader.loader.provider.game.impl.CosmicReachProvider",
            "dev.puzzleshq.puzzleloader.loader.provider.game.impl.MinecraftProvider",
            "dev.puzzleshq.puzzleloader.loader.provider.game.impl.ProjectZomboidProvider"
    };

    public final AtomicReference<EnvType> envType = new AtomicReference<>(EnvType.UNKNOWN);
    public IGameProvider gameProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger("Puzzle | FlexPiece");

    public static FlexPiece INSTANCE;

    public FlexPiece(EnvType type) {
        FlexPiece.INSTANCE = this;
        BootstrapPiece.generalClassloader = new FlexClassLoader("General-FlexClassLoader", new URL[0], BootstrapPiece.boostrapClassloader);
        BootstrapPiece.generalClassloader.addURL(ClassPathUtil.getJVMClassPathUrls());
        BootstrapPiece.generalClassloader.registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "net.neoforged.bus.");
        BootstrapPiece.generalClassloader.registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.");

        BootstrapPiece.boostrapClassloader.addChild(BootstrapPiece.generalClassloader);
        Thread.currentThread().setContextClassLoader(BootstrapPiece.generalClassloader);
        this.envType.set(type);
    }

    public static void launch(String[] args, EnvType type) {
        LoaderConstants.CLIConfiguration.COMMAND_LINE_ARGUMENTS = args;
        FlexPiece.INSTANCE = new FlexPiece(type);

        FlexPiece.INSTANCE.start(args);
    }

    private void start(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        try {
            OptionSpec<String> game_provider = parser.accepts("game-provider")
                    .withOptionalArg().ofType(String.class);

            OptionSpec<String> mod_folder = parser.accepts("mod-folder")
                    .withOptionalArg().ofType(String.class).defaultsTo(new File("pmods").getAbsolutePath());

            OptionSpec<String> mod_paths = parser.accepts("mod-paths")
                    .withOptionalArg().ofType(String.class);

            OptionSpec<Boolean> do_title_transformer = parser.accepts("do-title-transformer")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            OptionSpec<String> custom_title_format = parser.accepts("custom-title-formatter")
                    .withOptionalArg().ofType(String.class).defaultsTo("Puzzle Loader: %s");

            OptionSpec<Boolean> mixins_enabled = parser.accepts("mixins-enabled")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            OptionSpec<Boolean> transformers_enabled = parser.accepts("transformers-enabled")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            OptionSpec<Boolean> user_transformers_enabled = parser.accepts("user-transformers-enabled")
                    .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

            final OptionSet options = parser.parse(args);

            LoaderConstants.CLIConfiguration.DO_TITLE_TRANSFORMER = do_title_transformer.value(options);
            LoaderConstants.CLIConfiguration.CUSTOM_TITLE_FORMAT = custom_title_format.value(options);
            LoaderConstants.CLIConfiguration.MIXINS_ENABLED = mixins_enabled.value(options);
            LoaderConstants.CLIConfiguration.TRANSFORMERS_ENABLED = transformers_enabled.value(options);
            LoaderConstants.CLIConfiguration.USER_TRANSFORMERS_ENABLED = user_transformers_enabled.value(options) && transformers_enabled.value(options);

            if (options.has(mod_paths)) {
                String v = mod_paths.value(options);
                if (!v.contains(File.pathSeparator)) {
                    BootstrapPiece.generalClassloader.addURL(new File(v));
                } else {
                    String[] jars = mod_paths.value(options).split(File.pathSeparator);
                    for (String jar : jars) BootstrapPiece.generalClassloader.addURL(new File(jar));
                }
            }

            ModFinder.setModFolder(new File(mod_folder.value(options)).getAbsoluteFile());
            ModFinder.crawlModsFolder();

            gameProvider = null;

            if (options.has(game_provider)) {
                gameProvider = (IGameProvider) Class.forName(game_provider.value(options), true, BootstrapPiece.generalClassloader).newInstance();
            } else {
                for (String builtInProvider : BUILT_IN_PROVIDERS) {
                    gameProvider = (IGameProvider) Class.forName(builtInProvider, true, BootstrapPiece.generalClassloader).newInstance();
                    if (gameProvider.isValid()) break;
                }
            }
            if (!gameProvider.isValid())
                throw new RuntimeException("Couldn't load any game provider for this particular application.");

            ModFormats.register(2, new ModFormatV2());
            ModFormats.register(3, new ModFormatV3());
            ModFormats.register(ModFinder::getMod);
            ModFinder.findMods();

            AccessWriters.init(BootstrapPiece.generalClassloader);
            ModFinder.getAccessWriters(ModFinder.getModsArray());
            if (LoaderConstants.CLIConfiguration.USER_TRANSFORMERS_ENABLED)
                TransformerInitializer.invokeTransformers(BootstrapPiece.generalClassloader);

            gameProvider.initArgs(args);

            if (LoaderConstants.CLIConfiguration.TRANSFORMERS_ENABLED)
                gameProvider.registerTransformers(BootstrapPiece.generalClassloader);
            gameProvider.inject(BootstrapPiece.generalClassloader);

            if (LoaderConstants.CLIConfiguration.MIXINS_ENABLED) {
                MixinUtil.start();
                MixinUtil.doInit(new ArrayList<>());
                FlexPiece.setupModMixins();
                MixinUtil.inject();
                MixinUtil.goToPhase(MixinEnvironment.Phase.DEFAULT);

                BootstrapPiece.boostrapClassloader.registerTransformer(new IClassTransformer() {
                    @Override
                    public byte[] transform(String name, String fileName, byte[] bytes) {
                        if (name.contains("org.spongepowered.")) return bytes;
                        System.out.println(name);
                        BetterProxy proxy = new BetterProxy();
                        proxy.isActive = true;
                        return proxy.transform(name, fileName, bytes);
                    }
                });
                BootstrapPiece.generalClassloader.registerTransformer(new IClassTransformer() {
                    @Override
                    public byte[] transform(String name, String fileName, byte[] bytes) {
                        if (name.contains("org.spongepowered.")) return bytes;
                        System.out.println(name);
                        BetterProxy proxy = new BetterProxy();
                        proxy.isActive = true;
                        return proxy.transform(name, fileName, bytes);
                    }
                });
            }

            String entryPoint = gameProvider.getEntrypoint();
            String ranEntrypoint = entryPoint;
            if (entryPoint.contains("MinecraftApplet")) {
                ranEntrypoint = "dev.puzzleshq.puzzleloader.minecraft.launch.MinecraftAppletLauncher";
            }

            Class<?> clazz = Class.forName(ranEntrypoint, false, BootstrapPiece.generalClassloader);
            String[] providerArgs = gameProvider.getArgs().toArray(new String[0]);

            Method main = ReflectionUtil.getMethod(clazz, "main", String[].class);

            PreLaunchInitializer.invoke();
            OffThreadExecutor.start();
            LOGGER.info("Launching {} version {}", gameProvider.getName(), gameProvider.getRawVersion());
            main.invoke(null, (Object) providerArgs);
        } catch (Exception e) {
            LOGGER.error("Unable To Launch", e);
            System.exit(1);
        }
    }

    public static void setupModMixins() {
        List<MixinConfig> mixinConfigs = new ArrayList<>();
        Map<String, String> configToMod = new HashMap<>();
        for (IModContainer mod : ModFinder.getModsArray()) {
            for (MixinConfig mixinConfig : mod.getInfo().getMixinConfigs()) {
                mixinConfigs.add(mixinConfig);
                configToMod.put(mixinConfig.path(), mod.getID());
            }
        }


        EnvType envType = LoaderConstants.SIDE;
        mixinConfigs.forEach((e) -> {
            if (Objects.equals(envType.name, e.environment()) || Objects.equals(e.environment(), EnvType.UNKNOWN.name)) {
                Mixins.addConfiguration(e.path());
            }
        });

        for (Config config : Mixins.getConfigs()) {
            config.getConfig().decorate(FabricUtil.KEY_MOD_ID, configToMod.get(config.getName()));
        }
    }

}
