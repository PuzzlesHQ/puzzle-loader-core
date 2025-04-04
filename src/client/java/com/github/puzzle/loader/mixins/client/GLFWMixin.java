package com.github.puzzle.loader.mixins.client;

import com.github.puzzle.loader.util.GLFWUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static org.lwjgl.glfw.GLFW.nglfwCreateWindow;
import static org.lwjgl.glfw.GLFW.nglfwSetWindowTitle;

@Mixin(GLFW.class)
public abstract class GLFWMixin {

    /**
     * @author Mr_Zombii
     * @reason add "Puzzle Loader: " infront of titles.
     */
    @Overwrite
    @NativeType("GLFWwindow *")
    public static long glfwCreateWindow(int width, int height, @NativeType("char const *") CharSequence title, @NativeType("GLFWmonitor *") long monitor, @NativeType("GLFWwindow *") long share) {
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();

        long windowPtr;
        try {
            stack.nUTF8("Puzzle Loader: " + title, true);
            long titleEncoded = stack.getPointerAddress();
            windowPtr = nglfwCreateWindow(width, height, titleEncoded, monitor, share);
        } finally {
            stack.setPointer(stackPointer);
        }

        return windowPtr;
    }

    /**
     * @author Mr_Zombii
     * @reason add "Puzzle Loader: " infront of titles.
     */
    @Overwrite
    public static void glfwSetWindowTitle(long window, CharSequence title) {
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();

        try {
            stack.nUTF8("Puzzle Loader: " + title, true);
            long titleEncoded = stack.getPointerAddress();
            nglfwSetWindowTitle(window, titleEncoded);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

}
