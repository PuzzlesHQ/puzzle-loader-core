package dev.puzzleshq.loader.loading.events;

import dev.puzzleshq.loader.loading.GameLoader;
import dev.puzzleshq.loader.loading.stage.AbstractStage;

/**
 * An event that is called before the game-loader starts to collect game-loading stages.
 *
 * @author Mr_Zombii
 * @since 1.0.0
 *
 * @see GameLoader
 */
public class RegisterStagesEvent {

    public final GameLoader loader;

    public RegisterStagesEvent(GameLoader loader){
        this.loader = loader;
    }

    /**
     * A register method that adds the stage to the game-loader before execution,
     * all events under the package <code>com.github.puzzle.game.engine.stages</code> are prioritized and ran first.
     *
     * @author Mr_Zombii
     * @since 3.0.0
     *
     * @see GameLoader.Stage
     * @see AbstractStage
     */
    public void register(GameLoader.Stage stage) {
        loader.register(stage);
    }

}
