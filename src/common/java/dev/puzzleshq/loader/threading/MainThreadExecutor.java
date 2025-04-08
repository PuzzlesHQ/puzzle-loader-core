package dev.puzzleshq.loader.threading;

import javax.swing.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainThreadExecutor implements Runnable {

    public static final MainThreadExecutor INSTANCE = new MainThreadExecutor();

    protected Queue<Runnable> runnableQueue = new ConcurrentLinkedQueue<>();

    public static void add(Runnable runnable) {
        if (runnable instanceof MainThreadExecutor) return;

        INSTANCE.runnableQueue.add(runnable);
    }

    public static void start() {
        SwingUtilities.invokeLater(INSTANCE);
    }

    @Override
    public void run() {
        while (!runnableQueue.isEmpty()) {
            Runnable runnable = runnableQueue.poll();
            runnable.run();
        }
        SwingUtilities.invokeLater(INSTANCE);
    }
}
