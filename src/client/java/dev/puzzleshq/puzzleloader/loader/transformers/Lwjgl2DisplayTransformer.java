package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Lwjgl2DisplayTransformer extends ClassVisitor {

    protected Lwjgl2DisplayTransformer(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
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
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == Opcodes.GETSTATIC) {
                if (owner.equals("org.lwjgl.opengl.Display".replaceAll("\\.", "/")) && name.equals("title")) {
                    super.visitFieldInsn(opcode, owner, name, descriptor);
                    if (LoaderConstants.CLIConfiguration.DO_TITLE_TRANSFORMER) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/puzzleshq/puzzleloader/loader/LoaderConstants$CLIConfiguration", "formatTitle", "(Ljava/lang/CharSequence;)Ljava/lang/String;", false);
                    }
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "dev/puzzleshq/puzzleloader/minecraft/launch/MinecraftAppletLauncher", "setTitle", "(Ljava/lang/String;)Ljava/lang/String;", false);
                    return;
                }
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

}
