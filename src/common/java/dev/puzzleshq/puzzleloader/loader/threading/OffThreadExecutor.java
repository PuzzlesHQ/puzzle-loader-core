package dev.puzzleshq.puzzleloader.loader.threading;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OffThreadExecutor implements Runnable {

    public static final OffThreadExecutor INSTANCE = new OffThreadExecutor();

    protected Queue<Runnable> runnableQueue = new ConcurrentLinkedQueue<>();
    public static final Thread t;
    static {
        t = new Thread(INSTANCE);
        t.setName("OffThread Executor");
        t.setDaemon(true);
    }

    public static void add(Runnable runnable) {
        if (runnable instanceof OffThreadExecutor) return;

        INSTANCE.runnableQueue.add(runnable);
    }

    public static void start() {
        t.start();
    }

    @Override
    public void run() {
        while (true) {
            if (runnableQueue.isEmpty()) continue;
            Runnable runnable = runnableQueue.poll();
            runnable.run();
        }
    }
}
