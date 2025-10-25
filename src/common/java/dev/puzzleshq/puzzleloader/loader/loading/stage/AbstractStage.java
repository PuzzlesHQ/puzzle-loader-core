package dev.puzzleshq.puzzleloader.loader.loading.stage;

import dev.puzzleshq.puzzleloader.loader.loading.GameLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation of a game loading stage.
 * <p>
 * Provides common functionality for managing the stage name,
 * associated {@link GameLoader}, and a list of tasks to execute
 * during this stage.
 * </p>
 * <p>
 * Concrete stages should extend this class and add specific
 * stage behavior or additional tasks as needed.
 * </p>
 *
 */
public abstract class AbstractStage implements GameLoader.Stage {

    /** The game loader associated with this stage */
    protected GameLoader loader;

    /** The list of tasks to execute in this stage */
    protected List<Runnable> tasks = new ArrayList<>();

    /** The name of this stage */
    protected String name;

    /**
     * Constructs a new stage with the specified name.
     *
     * @param stageName the name of the stage
     */
    public AbstractStage(String stageName) {
        this.name = stageName;
    }

    /**
     * Returns the name of this stage.
     *
     * @return the stage name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the {@link GameLoader} instance for this stage.
     *
     * @param loader the game loader
     */
    @Override
    public void setGameLoader(GameLoader loader) {
        this.loader = loader;
    }

    /**
     * Returns the {@link GameLoader} associated with this stage.
     *
     * @return the game loader
     */
    @Override
    public GameLoader getGameLoader() {
        return loader;
    }

    /**
     * Returns the list of tasks for this stage.
     *
     * @return the stage tasks
     */
    @Override
    public List<Runnable> glTasks() {
        return tasks;
    }
}
