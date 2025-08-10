package dev.puzzleshq.puzzleloader.loader.provider.classloading;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.*;

import java.io.IOException;
import java.util.Collection;

public interface IFlexClassloader extends IClassTracker, ITransformerProvider, IClassProvider, IClassBytecodeProvider {

    void registerTransformer(ILegacyClassTransformer transformer);

    void registerTransformer(String transformerClassName);

    byte[] getClassBytes(String name) throws ClassNotFoundException;

    @Override
    default Collection<ITransformer> getDelegatedTransformers() {
        return getTransformers();
    }

    byte[] transformClass(byte[] bytes, String name, String classFileName);

    void registerRestriction(FlexClassRestriction flexClassRestriction, String s);

    String getName();
}
