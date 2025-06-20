package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;
import bundled.org.objectweb.asm.ClassReader;
import bundled.org.objectweb.asm.ClassWriter;

public class ClientASMTransformer implements IClassTransformer {

    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        String[] parts = name.split("\\.");

        if (
                name.equals("org.lwjgl.opengl.Display")
        ) {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

            reader.accept(new Lwjgl2DisplayTransformer(writer), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        }
        if (
                name.equals("org.lwjgl.glfw.GLFW")
        ) {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

            reader.accept(new GLFWTransformer(writer), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        }
        return basicClass;
    }

}
