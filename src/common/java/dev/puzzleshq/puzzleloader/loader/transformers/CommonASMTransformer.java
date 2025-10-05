package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.accesswriter.transformers.AccessTransformerASM;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class CommonASMTransformer implements IClassTransformer {
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) System.out.println(name);
        // errors with package-info.class
        if (!name.endsWith("package-info")) {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(0);

            reader.accept(new AccessTransformerASM(writer), 0);
            return writer.toByteArray();
        } else return null;
    }
}
