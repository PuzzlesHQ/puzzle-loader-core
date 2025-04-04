package com.github.puzzle.loader.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Lwjgl2DisplayTransformer extends ClassVisitor {

    protected Lwjgl2DisplayTransformer(ClassVisitor classVisitor) {
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

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("setTitle")) {
            return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    public static class MethodTransformer extends MethodVisitor {

        protected MethodTransformer(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }

        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
            super.visitFrame(type, numLocal, local, numStack, stack);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == Opcodes.GETSTATIC) {
                if (owner.equals("org.lwjgl.opengl.Display".replaceAll("\\.", "/")) && name.equals("title")) {
                    super.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                    super.visitInsn(Opcodes.DUP);
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                    super.visitLdcInsn("Puzzle Loader: ");
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                    super.visitFieldInsn(opcode, owner, name, descriptor);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
//                    visitInsn(Opcodes.ARETURN);
                    return;
                }
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

}
