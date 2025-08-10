package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapPiece;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.impl.FlexClassLoader;
import dev.puzzleshq.puzzleloader.loader.provider.mixin.service.FlexMixinService;
import dev.puzzleshq.puzzleloader.loader.provider.mixin.service.FlexMixinServiceBootstrap;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Sets the log4j config before any initialization to make sure logging works correctly.
 *
 * @author Mr_Zombii
 * @since 1.0.0
 *
*/
public class PrePiece {

    private static Logger LOGGER;

    public static void launch(String[] args, String type) {
        System.setProperty("mixin.bootstrapService", FlexMixinServiceBootstrap.class.getName());
        System.setProperty("mixin.service", FlexMixinService.class.getName());

        try {
            System.getProperties().setProperty(
                    "log4j.configurationFile",
                    Objects.requireNonNull(FlexClassLoader.class.getResource("/log4j2.xml")).toURI().toString()
            );
        } catch (URISyntaxException e) {
            getLogger().error("PrePiece seemed to have crashed when setting the LOG4J file, please contact a puzzle developer in the PuzzleHQ, https://discord.com/invite/XeVud4RC9U", e);
            System.exit(-14);
        }

        try {
            LoaderConstants.CLIConfiguration.BOOTSTRAPPED = BootstrapPiece.loadFlags();

            FlexPiece.launch(args, EnvType.valueOf(type));
        } catch (Exception e) {
            getLogger().error("Piece seemed to have crashed, please contact a puzzle developer in the PuzzleHQ, https://discord.com/invite/XeVud4RC9U", e);
            System.exit(-15);
        }
    }

    public static Logger getLogger() {
        if (LOGGER == null)
            LOGGER = LoggerFactory.getLogger("Puzzle | Emergency Piece");
        return LOGGER;
    }
}
