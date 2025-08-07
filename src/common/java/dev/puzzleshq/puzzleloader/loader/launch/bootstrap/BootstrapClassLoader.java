package dev.puzzleshq.puzzleloader.loader.launch.bootstrap;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;
import dev.puzzleshq.puzzleloader.loader.transformers.MixinProxyTransformer;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class BootstrapClassLoader extends URLClassLoader {

    public static final List<IClassTransformer> transformers = new ArrayList<>();
    public static boolean dumpClasses;

    public BootstrapClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public static final Set<String> missingClasses = new HashSet<>();
    public static final Map<String, Class<?>> classCache = new HashMap<>();

    public static final Set<String> missingResourceCache = new HashSet<>();
    public static final Map<String, byte[]> resourceCache = new HashMap<>();

    public static final Set<String> excludedClasses = new HashSet<>();
    public static final Set<String> transformerExcludedClasses = new HashSet<>();

    public static final File classOverridesDir = new File("class-overrides");
    public static final File classDumpDir = new File("class-transform-dump");

    public static boolean overrides = false;

    {
        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("jdk.");
        addClassLoaderExclusion("com.sun.");
        addClassLoaderExclusion("org.xml.");
        addClassLoaderExclusion("javax.");
        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("org.apache.logging.");
        addClassLoaderExclusion("org.slf4j");

        BootstrapClassLoader.transformers.add(
                ((name, fileName, bytes) -> {
                    if (!name.equals("org.spongepowered.asm.mixin.transformer.MixinTransformer")) {
                        return bytes;
                    }
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

                    reader.accept(new MixinProxyTransformer(writer), ClassReader.EXPAND_FRAMES);
                    return writer.toByteArray();
                })
        );
    }

    public void addClassLoaderExclusion(String s) {
        excludedClasses.add(s);
    }

    public void addTransformerExclusion(String s) {
        transformerExcludedClasses.add(s);
    }

    private URLConnection getConnection(String name) {
        final URL resource = findResource(name);
        if (resource != null) {
            try {
                return resource.openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (missingClasses.contains(name)) throw new ClassNotFoundException(name);

        for (String exclusion : excludedClasses) {
            if (name.startsWith(exclusion)) return getParent().loadClass(name);
        }

        if (isClassLoaded(name))
            return classCache.get(name);

        for (String exclusion : transformerExcludedClasses) {
            if (name.startsWith(exclusion)) {
                try {
                    Class<?> clazz = super.findClass(name);
                    classCache.put(name, clazz);
                    return clazz;
                } catch (ClassNotFoundException e) {
                    missingClasses.add(name);
                    throw e;
                }
            }
        }

        try {
            if (isClassLoaded(name))
                return classCache.get(name);

            int lastDot = name.lastIndexOf('.');
            String pkgName = lastDot == -1 ? "" : name.substring(0, lastDot);

            String fileName = toFileName(name);

            URLConnection connection = getConnection(fileName);
            CodeSigner[] signers = null;

            if (lastDot > -1) {

                if (connection instanceof JarURLConnection jarConnection) {
                    JarFile jar = jarConnection.getJarFile();

                    if (jar != null && jar.getManifest() != null) {
                        Manifest manifest = jar.getManifest();
                        JarEntry entry = jar.getJarEntry(fileName);

                        getResourceBytes(name);
                        signers = entry.getCodeSigners();
                        if (getPackage(pkgName) == null)
                            definePackage(pkgName, manifest, jarConnection.getJarFileURL());
                    }
                } else {
                    Package pkg = getPackage(pkgName);
                    if (pkg == null)
                        definePackage(pkgName, null, null, null, null, null, null, null);
                }
            }

            byte[] bytes = transform(name, name, getResourceBytes(name));
            if (bytes == null) {
                missingClasses.add(name);
                throw new ClassNotFoundException(name);
            }
            CodeSource source = connection == null ? null : new CodeSource(connection.getURL(), signers);
            Class<?> clazz = defineClass(name, bytes, 0, bytes.length, source);
            classCache.put(name, clazz);
            return clazz;
        } catch (IOException e) {
            missingClasses.add(name);
            throw new ClassNotFoundException(name, e);
        }
    }

    private byte[] transform(String name, String fileName, byte[] bytes) {
        byte[] transformed = bytes;
        for (IClassTransformer transformer : transformers) {
            transformed = transformer.transform(fileName, name, transformed);
        }
        if (transformed != bytes) {
            outputClass(name, transformed);
        }
        return transformed;
    }

    public static void outputClass(String name, byte[] transformed) {
        System.out.println(dumpClasses);
        if (!dumpClasses) return;
        try {
            File output = new File(classDumpDir, name.replaceAll("\\.", "/") + ".class");
            if (!output.getParentFile().exists()) output.getParentFile().mkdirs();
            if (!output.exists()) output.createNewFile();

            FileOutputStream stream = new FileOutputStream(output);
            stream.write(transformed);
            stream.close();
        } catch (IOException ignore) {}
    }

    public byte[] getResourceBytes(String name) {
        if (overrides) {
            RawAssetLoader.RawFileHandle handle = RawAssetLoader.getLowLevelRelativeAssetErrors(classOverridesDir, "/".concat(toFileName(name)), false);
            if (handle != null) {
                byte[] bytes = handle.getBytes();
                resourceCache.put(name, bytes);
                handle.dispose();
                return bytes;
            }
        }

        if (missingResourceCache.contains(name)) return null;
        RawAssetLoader.RawFileHandle handle = RawAssetLoader.getLowLevelClassPathAssetErrors("/".concat(toFileName(name)), false);

        if (handle == null) {
            missingResourceCache.add(name);
            return null;
        }

        byte[] bytes = handle.getBytes();
        resourceCache.put(name, bytes);
        handle.dispose();
        return bytes;
    }

    private String toFileName(String name) {
        return name.replace('.', '/').concat(".class");
    }

    private boolean isClassLoaded(String name) {
        return classCache.containsKey(name);
    }

    public static void usesOverrides(boolean overrides) {
        if (!classOverridesDir.exists()) classOverridesDir.mkdirs();
        BootstrapClassLoader.overrides = overrides;
    }

    public static void dumps(boolean dumpClasses) {
        BootstrapClassLoader.dumpClasses = dumpClasses;
    }
}
