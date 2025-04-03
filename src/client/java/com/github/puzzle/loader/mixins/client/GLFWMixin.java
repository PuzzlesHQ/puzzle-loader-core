package com.github.puzzle.loader.mixins.client;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        long var11;
        try {
            stack.nUTF8("Puzzle Loader: " + title, true);
            LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
            long titleEncoded = stack.getPointerAddress();
            var11 = nglfwCreateWindow(width, height, titleEncoded, monitor, share);
        } finally {
            stack.setPointer(stackPointer);
        }

        return var11;
    }

    /**
     * @author Mr_Zombii
     * @reason add "Puzzle Loader: " infront of titles.
     */
    @Inject(method = "glfwSetWindowTitle(JLjava/lang/CharSequence;)V", at = @At("HEAD"), cancellable = true)
    private static void glfwSetWindowTitle(long window, CharSequence title, CallbackInfo ci) {
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));
        LoggerFactory.getLogger("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").info(String.valueOf(title));

        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();

        try {
            stack.nUTF8("Puzzle Loader: " + title, true);
            long titleEncoded = stack.getPointerAddress();
            nglfwSetWindowTitle(window, titleEncoded);
        } finally {
            stack.setPointer(stackPointer);
        }
        ci.cancel();

    }

}
