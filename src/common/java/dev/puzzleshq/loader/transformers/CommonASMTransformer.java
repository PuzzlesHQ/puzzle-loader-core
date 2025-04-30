package dev.puzzleshq.loader.transformers;

import bundled.org.objectweb.asm.ClassReader;
import bundled.org.objectweb.asm.ClassWriter;
import dev.puzzleshq.accesswriter.transformers.AccessTransformer;
import dev.puzzleshq.loader.launch.fix.IClassTransformer;

// TODO: read access-writers from mod.
public class CommonASMTransformer implements IClassTransformer {

    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) System.out.println(name);
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        reader.accept(new AccessTransformer(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

}
