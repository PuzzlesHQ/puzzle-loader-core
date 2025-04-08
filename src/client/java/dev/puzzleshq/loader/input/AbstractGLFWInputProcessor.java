package dev.puzzleshq.loader.input;

public abstract class AbstractGLFWInputProcessor implements GLFWInputProcessor {

    public abstract void onScroll(long window, double xOffset, double yOffset);
    public abstract void onCursorEnter(long window, boolean entered);
    public abstract void onMouseClick(long window, int button, int action, int mods);
    public abstract void onCursorMove(long window, double x, double y);

    public abstract void onKeyPress(long window, int key, int scancode, int action, int mods);
    public abstract void onCharTyped(long window, int codepoint);

    public abstract void onFileDropped(long window, int count, long paths);

}
