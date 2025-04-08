package dev.puzzleshq.loader.input;

public interface GLFWInputProcessor {

    void onScroll(long window, double xOffset, double yOffset);
    void onCursorEnter(long window, boolean entered);
    void onMouseClick(long window, int button, int action, int mods);
    void onCursorMove(long window, double x, double y);
    void onKeyPress(long window, int key, int scancode, int action, int mods);
    void onCharTyped(long window, int codepoint);
    void onFileDropped(long window, int count, long paths);

}
