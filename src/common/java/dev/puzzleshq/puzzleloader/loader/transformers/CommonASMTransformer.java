package dev.puzzleshq.puzzleloader.loader.transformers;

import bundled.org.objectweb.asm.ClassReader;
import bundled.org.objectweb.asm.ClassWriter;
import dev.puzzleshq.accesswriter.transformers.AccessTransformerASM;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;

// TODO: read access-writers from mod.
public class CommonASMTransformer implements IClassTransformer {

    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) System.out.println(name);
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        reader.accept(new AccessTransformerASM(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

}
