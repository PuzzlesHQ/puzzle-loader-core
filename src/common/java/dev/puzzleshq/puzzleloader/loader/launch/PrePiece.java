package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.puzzleloader.loader.provider.mixin.PuzzleLoaderMixinService;
import dev.puzzleshq.puzzleloader.loader.provider.mixin.PuzzleLoaderMixinServiceBootstrap;
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

    public static void launch(String[] args, EnvType type) {
        System.setProperty("mixin.bootstrapService", PuzzleLoaderMixinServiceBootstrap.class.getName());
        System.setProperty("mixin.service", PuzzleLoaderMixinService.class.getName());

        try {
            System.getProperties().setProperty("log4j.configurationFile", Objects.requireNonNull(PieceClassLoader.class.getResource("/log4j2.xml")).toURI().toString());
        } catch (URISyntaxException e) {
            getLogger().error("Uh Oh stinky, an Illegal Error happened.", e);
            System.exit(-69);
        }

        try {
            Piece.launch(args, type);
        } catch (Exception e) {
            getLogger().error("Piece seemed to have crashed, please contact a ⚙️Mod⚙️ Dev or 🧩Puzzle🧩 Dev near 🫵 to resolve this error 😎", e);
            System.exit(-69);
        }
    }

    public static Logger getLogger() {
        if (LOGGER == null)
            LOGGER = LoggerFactory.getLogger("Puzzle | Emergency Piece");
        return LOGGER;
    }
}
