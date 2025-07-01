package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import dev.puzzleshq.accesswriter.transformers.AccessTransformerASM;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;

// TODO: read access-writers from mod.
public class CommonASMTransformer implements IClassTransformer {
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) System.out.println(name);
        // errors with package-info.class
        if (!name.endsWith("package-info")) {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

            reader.accept(new AccessTransformerASM(writer), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } else return null;
    }
}
