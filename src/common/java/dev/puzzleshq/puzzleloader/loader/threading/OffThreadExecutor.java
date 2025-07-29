package dev.puzzleshq.puzzleloader.loader.threading;

import javax.swing.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OffThreadExecutor implements Runnable {

    public static final OffThreadExecutor INSTANCE = new OffThreadExecutor();

    protected Queue<Runnable> runnableQueue = new ConcurrentLinkedQueue<>();
    public static Thread t;

    public static void add(Runnable runnable) {
        if (runnable instanceof OffThreadExecutor) return;

        INSTANCE.runnableQueue.add(runnable);
    }

    public static void start() {
        SwingUtilities.invokeLater(INSTANCE);
    }

    @Override
    public void run() {
        t = Thread.currentThread();
        t.setName("OffThread Executor");
        while (!runnableQueue.isEmpty()) {
            Runnable runnable = runnableQueue.poll();
            runnable.run();
        }
        SwingUtilities.invokeLater(INSTANCE);
    }
}
