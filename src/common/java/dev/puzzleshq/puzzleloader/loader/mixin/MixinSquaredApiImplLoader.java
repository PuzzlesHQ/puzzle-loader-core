package dev.puzzleshq.puzzleloader.loader.mixin;

import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster;
import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import dev.puzzleshq.puzzleloader.loader.provider.ProviderException;
import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loader for Mixin Squared API implementations.
 * <p>
 * Handles discovery and registration of {@link MixinCanceller} and
 * {@link MixinAnnotationAdjuster} implementations provided by mods.
 * </p>
 * <p>
 * Uses {@link PuzzleEntrypointUtil} to retrieve entrypoints and registers
 * them with {@link MixinCancellerRegistrar} and
 * {@link MixinAnnotationAdjusterRegistrar}.
 * </p>
 *
 */
public class MixinSquaredApiImplLoader {

    private static final Logger LOGGER = LogManager.getLogger("Mixin Squared");

    public static void load() {
        for (PuzzleEntrypointUtil.Entrypoint<MixinCanceller> container : PuzzleEntrypointUtil.getEntrypoints("mixinsquared", MixinCanceller.class)) {
            String id = container.getProvider().getID();
            try {
                MixinCanceller canceller = container.createInstance();
                MixinCancellerRegistrar.register(canceller);
            } catch (ProviderException e) {
                LOGGER.error("Mod {} providers a broken MixinCanceller implementation:\n", id, e);
            }
        }
        for (PuzzleEntrypointUtil.Entrypoint<MixinAnnotationAdjuster> container : PuzzleEntrypointUtil.getEntrypoints("mixinsquared-adjuster", MixinAnnotationAdjuster.class)) {
            String id = container.getProvider().getID();
            try {
                MixinAnnotationAdjuster annotationAdjuster = container.createInstance();
                MixinAnnotationAdjusterRegistrar.register(annotationAdjuster);
            } catch (ProviderException e) {
                LOGGER.error("Mod {} providers a broken MixinAnnotationAdjuster implementation:\n", id, e);
            }
        }
    }

}
