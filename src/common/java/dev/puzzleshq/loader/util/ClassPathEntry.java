package dev.puzzleshq.loader.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPathEntry {

    File file;
    boolean isArchive, isDirectory;

    ClassPathEntry(
            File file,
            boolean isArchive,
            boolean isDirectory
    ) {
        this.file = file;
        this.isArchive = isArchive;
        this.isDirectory = isDirectory;
    }

    public File file() {
        return file;
    }

    public boolean isArchive() {
        return isArchive;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public byte[] getContents() throws IOException {
        InputStream stream = null;
        byte[] bytes = new byte[0];

        if (file.canRead() && file.isFile()) stream = new FileInputStream(file);
        if (stream == null) throw new IOException("Could Not Read \"" + file.getName() + "\"");

        bytes = NativeArrayUtil.readNBytes(stream, Integer.MAX_VALUE);
        stream.close();
        return bytes;
    }

    public ClassPathFileEntry[] listAllFiles() throws IOException {
        List<ClassPathFileEntry> entries = new ArrayList<>();

        if (isArchive) {
            ZipFile zip = new ZipFile(file);
            Enumeration<? extends ZipEntry> iterator = zip.entries();
            while (iterator.hasMoreElements()) {
                ZipEntry entry = iterator.nextElement();

                entries.add(new ClassPathFileEntry(
                        true,
                        entry.getName(),
                        entry,
                        zip,
                        null
                ));
            }
        } else {
            if (!isDirectory) throw new IOException("Cannot get files from non-archive/directory");

            for (File file : ModLocator.getFilesRecursive(file)) {
                entries.add(new ClassPathFileEntry(
                        false,
                        file.getName(),
                        null,
                        null,
                        file
                ));
            }
        }

        return entries.toArray(new ClassPathFileEntry[0]);
    }

}
