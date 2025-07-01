package dev.puzzleshq.puzzleloader.loader.transformers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class GLFWTransformer extends ClassVisitor {

    protected GLFWTransformer(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name.equals("glfwSetWindowTitle") && descriptor.equals("(JLjava/lang/CharSequence;)V")) {
            return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions), 2);
        }
        if (name.equals("glfwCreateWindow") && descriptor.equals("(IILjava/lang/CharSequence;JJ)J")) {
            return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions), 2);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    public static class MethodTransformer extends MethodVisitor {

        private final int index;

        protected MethodTransformer(MethodVisitor methodVisitor, int index) {
            super(Opcodes.ASM9, methodVisitor);
            this.index = index;
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            if (opcode == Opcodes.ALOAD && varIndex == index) {
                super.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                super.visitInsn(Opcodes.DUP);
                super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                super.visitLdcInsn("Puzzle Loader: ");
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                super.visitVarInsn(opcode, varIndex);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;", false);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                return;
            }
            super.visitVarInsn(opcode, varIndex);
        }
    }

}
