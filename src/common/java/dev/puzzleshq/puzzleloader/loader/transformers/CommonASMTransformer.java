package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import dev.puzzleshq.accesswriter.transformers.AccessTransformerASM;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;

// TODO: read access-writers from mod.
public class CommonASMTransformer implements IClassTransformer {

    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.endsWith("package-info.class")) return RawAssetLoader.getLowLevelClassPathAsset(transformedName).getBytes();
        if (basicClass == null) return null;
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        reader.accept(new AccessTransformerASM(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

}
