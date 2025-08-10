package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.accesswriter.AccessWriters;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import dev.puzzleshq.accesswriter.transformers.AccessTransformerASM;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;

public class CommonASMTransformer implements IClassTransformer {
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) System.out.println(name);
        // errors with package-info.class
        if (!name.endsWith("package-info")) {
            if (!AccessWriters.MERGED.has(transformedName)) return basicClass;

            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(0);

            reader.accept(new AccessTransformerASM(writer), 0);
            return writer.toByteArray();
        } else return null;
    }
}
