package dev.puzzleshq.puzzleloader.loader.util.proxy;

import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class ProxyClassUtil {

    private static final String MAIN_METHOD_NAME = "main";
    private static final String MAIN_METHOD_DESC = "([Ljava/lang/String;)V";

    public static Class<?> createAndLoadProxyInvoker(String clazz) {
        ClassNode node = getNode(clazz);
        String[] exceptions = getExceptions(node).toArray(new String[0]);

        String proxyClassName = "dev.puzzleshq.loader.proxyclass.GameLoaderProxy";

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ProxyClassWriter proxyClassWriter = new ProxyClassWriter(writer, isInterface(node), clazz.replaceAll("\\.", "/"), exceptions);

        proxyClassWriter.visit(
                Opcodes.V1_8, Opcodes.ACC_PUBLIC,
                proxyClassName.replaceAll("\\.", "/"), null,
                Object.class.getName().replaceAll("\\.", "/"),
                new String[0]
        );

        byte[] proxyClassBytes = writer.toByteArray();

        return Piece.classLoader.defineClass(proxyClassName, proxyClassBytes);
    }

    private static ClassNode getNode(String clazz) {
        String classPath = clazz.replaceAll("\\.", "/") + ".class";
        RawAssetLoader.RawFileHandle handle = RawAssetLoader.getLowLevelClassPathAsset(classPath);

        byte[] bytes = handle.getBytes();
        ClassNode node = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static List<String> getExceptions(ClassNode node) {
        MethodNode mainMethodNode = null;
        for (MethodNode method : node.methods) {
            if (method.name.equals(MAIN_METHOD_NAME) && method.desc.equals(MAIN_METHOD_DESC)) {
                mainMethodNode = method;
                break;
            }
        }

        assert mainMethodNode != null;
        return mainMethodNode.exceptions;
    }

    private static boolean isInterface(ClassNode node) {
        return (node.access & Opcodes.ACC_INTERFACE) != 0;
    }

    private static class ProxyClassWriter extends ClassVisitor {
        private final boolean isInterface;
        private final String proxiedClass;
        private final String[] exceptions;

        protected ProxyClassWriter(ClassVisitor visitor, boolean isInterface, String proxiedClass, String[] exceptions) {
            super(Opcodes.ASM9, visitor);
            this.isInterface = isInterface;
            this.proxiedClass = proxiedClass;
            this.exceptions = exceptions;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            MethodVisitor mw = visitMethod(
                    Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                    MAIN_METHOD_NAME, MAIN_METHOD_DESC,
                    null, exceptions
            );
            mw.visitCode();
            mw.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "dev/puzzleshq/puzzleloader/loader/util/PuzzleEntrypointInstantiator",
                    "createAllModEntryPointInstances",
                    "()V",
                    false
            );
            mw.visitVarInsn(Opcodes.ALOAD, 0);
            mw.visitMethodInsn(
                    isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKESTATIC,
                    proxiedClass,
                    MAIN_METHOD_NAME, MAIN_METHOD_DESC,
                    isInterface
            );
            mw.visitInsn(Opcodes.RETURN);
            mw.visitMaxs(1, 1);
            mw.visitEnd();
        }

    }

}
