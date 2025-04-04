package com.github.puzzle.loader.util;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiConsumer;

public class GLFWUtil {

    private static final double[] xStatic = new double[1];
    private static final double[] yStatic = new double[1];

    public static Vector2f getMousePos(long window) {
        GLFW.glfwGetCursorPos(window, xStatic, yStatic);
        return new Vector2f((float) xStatic[0], (float) yStatic[0]);
    }

}
