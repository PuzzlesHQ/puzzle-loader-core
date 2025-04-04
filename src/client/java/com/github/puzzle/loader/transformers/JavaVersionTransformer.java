package com.github.puzzle.loader.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class JavaVersionTransformer extends ClassVisitor {

    protected JavaVersionTransformer(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (version <= Opcodes.V1_5) {
            super.visit(Opcodes.V9, access, name, signature, superName, interfaces);
            return;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }
}
