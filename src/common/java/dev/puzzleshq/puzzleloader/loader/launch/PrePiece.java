package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Pre-initialization class for Puzzle Loader that sets up Log4j and
 * Mixins before the main Piece loader is launched.
 * <p>
 * Responsible for configuring logging, bootstrap mixin services,
 * mixin constraints, and forwarding control to {@link Piece}.
 * </p>
 * <p>
 * Ensures that logging works even during early loading stages.
 * Any failure in setup or launch will terminate the application with
 * a specific exit code.
 * </p>
 *
 * @author Mr_Zombii
 * @since 1.0.0
 */
public class PrePiece {

    /** Logger used for early initialization and emergency messages */
    private static Logger LOGGER;

    /**
     * Launches the Puzzle Loader with the specified arguments and environment type.
     * <p>
     * Sets Mixins system properties, configures Log4j, and delegates to {@link Piece#launch(String[], EnvType)}.
     * Any critical failure will log the error and terminate the JVM.
     * </p>
     *
     * @param args Command-line arguments
     * @param type The environment type as a string (must match {@link EnvType} enum)
     */
    public static void launch(String[] args, String type) {
        System.setProperty("mixin.bootstrapService", "dev.puzzleshq.puzzleloader.loader.provider.mixin.PuzzleLoaderMixinServiceBootstrap");
        System.setProperty("mixin.service", "dev.puzzleshq.puzzleloader.loader.provider.mixin.PuzzleLoaderMixinService");
        System.setProperty("mixinconstraints.abstraction", "dev.puzzleshq.puzzleloader.loader.mixin.PuzzleAbstractionsImpl");
        System.setProperty("mixinconstraints.verbose", "true");

        try {
            System.getProperties().setProperty(
                    "log4j.configurationFile",
                    Objects.requireNonNull(PrePiece.class.getResource("/log4j2.xml")).toURI().toString()
            );
        } catch (URISyntaxException e) {
            getLogger().error(
                    "PrePiece crashed when setting the LOG4J file, please contact a PuzzleHQ developer: https://discord.com/invite/XeVud4RC9U",
                    e
            );
            System.exit(-14);
        }

        try {
            Piece.launch(args, EnvType.valueOf(type));
        } catch (Exception e) {
            getLogger().error(
                    "Piece crashed, please contact a PuzzleHQ developer: https://discord.com/invite/XeVud4RC9U",
                    e
            );
            System.exit(-15);
        }
    }

    /**
     * Returns the emergency logger instance.
     * Initializes the logger if it does not already exist.
     *
     * @return Logger instance
     */
    public static Logger getLogger() {
        if (LOGGER == null)
            LOGGER = LogManager.getLogger("Puzzle | Emergency Piece");
        return LOGGER;
    }
}
