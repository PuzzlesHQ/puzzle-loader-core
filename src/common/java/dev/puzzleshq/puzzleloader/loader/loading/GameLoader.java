package dev.puzzleshq.puzzleloader.loader.loading;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.loading.events.EventGameLoaderFinish;
import dev.puzzleshq.puzzleloader.loader.loading.events.EventRegisterStages;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.GameLoaderEntrypoint;
import dev.puzzleshq.puzzleloader.loader.util.PuzzleEntrypointUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles the staged game loading process, coordinating stage registration, execution,
 * progress tracking, and cleanup.
 * <p>
 * The {@code GameLoader} maintains separate queues for core and external stages,
 * executes them in sequence, and invokes entrypoint callbacks during stage registration
 * and when loading completes.
 * </p>
 */
public class GameLoader {

    private static final Logger LOGGER = LogManager.getLogger("Puzzle | GameLoader");

    /** Holds all active GameLoader instances. */
    public static final ArrayList<GameLoader> INSTANCES = new ArrayList<>();

    /** Indicates whether this GameLoader has completed its loading sequence. */
    public final AtomicBoolean finished = new AtomicBoolean();

    /** Signals whether the loader should terminate early. */
    private final AtomicBoolean shouldClose = new AtomicBoolean();

    /** Queue of all stages scheduled for execution. */
    private final Queue<Stage> stages = new LinkedList<>();

    /** Queue of tasks executed on the main thread during loading. */
    private final Queue<Runnable> glQueue = new LinkedList<>();

    /** Primary progress bars for visual tracking during loading. */
    public ProgressBar bar1;
    public ProgressBar bar2;
    public ProgressBar bar3;

    /** Internal lists for separating core and external stages. */
    private final List<Stage> coreStages = new ArrayList<>();
    private final List<Stage> otherStages = new ArrayList<>();

    /** Registers a new {@code GameLoader} instance globally. */
    public GameLoader() {
        INSTANCES.add(this);
    }

    /**
     * Initializes the game loader, registers stages via entrypoints,
     * and starts the asynchronous loading thread.
     */
    public void create() {
        bar1.setVisible(false);
        bar2.setVisible(false);
        bar3.setVisible(false);

        EventRegisterStages registerStages = new EventRegisterStages(this);

        PuzzleEntrypointUtil.invoke(
                GameLoaderEntrypoint.ENTRYPOINT_KEY,
                GameLoaderEntrypoint.class,
                (init) -> init.onGameLoadingStageRegister(registerStages)
        );

        Thread thread = new Thread(this::gameLoadingThread, "Game-Loader");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Registers a stage for execution, distinguishing between core and external stages.
     *
     * @param stage the stage to register
     */
    public void register(Stage stage) {
        if (
                stage.getClass().getPackage().getName().startsWith("com.github.puzzle") ||
                        stage.getClass().getPackage().getName().startsWith("io.github.puzzle") ||
                        stage.getClass().getPackage().getName().startsWith("dev.puzzleshq")
        ) {
            coreStages.add(stage);
            return;
        }

        otherStages.add(stage);
    }

    /**
     * Executes a batch of queued {@link Runnable} tasks for a limited duration,
     * ensuring consistent main-thread updates.
     */
    public void update() {
        long endTime = System.currentTimeMillis() + 50;
        while (!glQueue.isEmpty() && System.currentTimeMillis() < endTime) {
            Runnable glTask = glQueue.poll();
            if (glTask != null) {
                glTask.run();
            }
        }
    }

    /**
     * The main game-loading process that executes all registered stages sequentially.
     * Each stage is run in its own synchronized context and may enqueue main-thread tasks.
     */
    public void gameLoadingThread() {
        stages.addAll(coreStages);
        stages.addAll(otherStages);
        coreStages.clear();
        otherStages.clear();

        bar1.setVisible(true);
        bar1.setMax(stages.size());
        int stagesCount = stages.size();
        int count = 0;
        while (!stages.isEmpty()) {
            if (LoaderConstants.shouldClose() || shouldClose.get()) {
                System.out.println("Cleaning up && Shutting down game-loader");
                cleanup();
                return;
            }

            CountDownLatch lock = new CountDownLatch(1);
            Stage stage = stages.poll();

            bar1.setProgress(++count);
            bar1.setText(String.format("Stage §[03bfd4]%d§[008a99]/§[03bfd4]%d§r: %s", count, stagesCount, stage.getName()));

            assert stage != null;
            stage.setGameLoader(this);
            stage.doStage();
            glQueue.addAll(stage.glTasks());
            glQueue.add(lock::countDown);

            try {
                lock.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.gc();
        }

        EventGameLoaderFinish gameLoaderFinish = new EventGameLoaderFinish();

        PuzzleEntrypointUtil.invoke(
                GameLoaderEntrypoint.ENTRYPOINT_KEY,
                GameLoaderEntrypoint.class,
                (init) -> init.onGameLoadingFinish(gameLoaderFinish)
        );

        this.cleanup();

        System.out.println("Finished Game Loading");
        finished.set(true);
    }

    /** Cleans up progress bars and stage lists after loading completes or aborts. */
    private void cleanup() {
        bar1 = null;
        bar2 = null;
        bar3 = null;

        stages.clear();
        coreStages.clear();
        otherStages.clear();
    }

    /** Requests that the current GameLoader stops processing and shuts down gracefully. */
    public void close() {
        shouldClose.set(true);
    }

    /** Forces all active GameLoader instances to terminate. */
    public static void killAll() {
        for (GameLoader loader : INSTANCES) {
            loader.close();
        }
    }

    /**
     * Represents a single game loading stage.
     * Each stage defines its own logic and main-thread task queue.
     */
    public interface Stage {

        void setGameLoader(GameLoader loader);
        GameLoader getGameLoader();

        void doStage();

        List<Runnable> glTasks();

        String getName();
    }

    /**
     * Represents a visual progress bar displayed during the game-loading process.
     */
    public interface ProgressBar {

        /** A dummy progress bar that performs no operations. */
        ProgressBar NULL_BAR = new ProgressBar() {
            @Override
            public void setText(String s) {}

            @Override
            public void setProgress(float progress) {}

            @Override
            public void setMax(int max) {}

            @Override
            public void setVisible(boolean b) {}
        };

        void setText(String s);
        void setProgress(float progress);
        void setMax(int max);

        void setVisible(boolean b);
    }

}
