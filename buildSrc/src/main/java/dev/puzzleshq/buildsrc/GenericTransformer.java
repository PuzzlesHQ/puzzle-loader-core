package dev.puzzleshq.buildsrc;

import dev.puzzleshq.accesswriter.transformers.AccessTransformer24;
import dev.puzzleshq.accesswriter.transformers.AccessTransformerASM;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class GenericTransformer {

    public static void transform(File out) throws IOException {
        try {
            InputStream byteStream = new FileInputStream(out);
            byte[] bytes = byteStream.readAllBytes();
            byteStream.close();

            ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes));
            ZipOutputStream output = new ZipOutputStream(new FileOutputStream(out));
            ZipEntry entry = input.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();

                if (entryName.contains("module-info") || entryName.contains("package-info")) {
                    output.putNextEntry(entry);
                    output.write(input.readAllBytes());
                } else if (entryName.endsWith(".class")) {
                    byte[] classBytes = transformClass(input.readAllBytes());
                    if (classBytes != null) {
                        output.putNextEntry(entry);
                        output.write(classBytes);
                    }
                } else {
                    output.putNextEntry(entry);
                    output.write(input.readAllBytes());
                }

                entry = input.getNextEntry();
            }
            input.close();
            output.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] transformClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        reader.accept(new AccessTransformerASM(writer), ClassReader.SKIP_DEBUG);

        return writer.toByteArray();
    }

//    private static byte[] transformClass(byte[] bytes) {
//        return AccessTransformer24.transform(bytes);
//    }

//    private static byte[] transformClass(byte[] bytes, AbstractClassTransformer... transformers) {
//        ClassReader reader = new ClassReader(bytes);
//        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
//
//        for (AbstractClassTransformer transformer : transformers) {
//            transformer.setWriter(writer);
//            transformer.setClassName(reader.getClassName().replaceAll("\\.", "/"));
//            reader.accept(transformer, 2);
//        }
//
//        return writer.toByteArray();
//    }

}
