package dev.puzzleshq.puzzleloader.loader.threading;

import java.io.*;

public class ThreadExceptionCatcher implements Thread.UncaughtExceptionHandler {

    private static final ThreadExceptionCatcher catcher = new ThreadExceptionCatcher();

    public static Thread createThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(name);
        attach(thread);
        return thread;
    }

    public static void attach(Thread thread) {
        thread.setUncaughtExceptionHandler(catcher);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        OutputStream stream = new PrintStream(dataStream);
        PrintWriter writer = new PrintWriter(stream);
        writer.println("---- [ " + t.getName() + " ] ----");

        try {
            byte[] bytes = dataStream.toByteArray();
            System.err.writeBytes(bytes);

            File file = new File("thread-exception-log--" + t.getName() + ".txt");
            if (!file.exists()) file.createNewFile();
            FileOutputStream stream1 = new FileOutputStream(file);
            stream1.write(bytes, 0, bytes.length);
            stream1.close();
        } catch (Exception ignore) {
        }
    }
}
