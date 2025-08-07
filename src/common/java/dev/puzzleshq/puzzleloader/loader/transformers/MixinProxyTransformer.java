package dev.puzzleshq.puzzleloader.loader.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

public class MixinProxyTransformer extends ClassVisitor {

    public MixinProxyTransformer(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (Objects.equals(name, "<init>")) {
            return super.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, signature, exceptions);
        }
        if (Objects.equals(name, "gotoPhase") && descriptor.equals("(Lorg/spongepowered/asm/mixin/MixinEnvironment$Phase;)V")) {
            return super.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, signature, exceptions);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
