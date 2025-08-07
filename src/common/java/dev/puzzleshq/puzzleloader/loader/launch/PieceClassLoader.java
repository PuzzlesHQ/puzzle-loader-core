package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapClassLoader;
import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.ITransformer;

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

public class PieceClassLoader extends URLClassLoader implements IClassTracker {

    public final ClassLoader parent = getClass().getClassLoader();

    public final List<URL> sources = new ArrayList<>();

    public final Set<String> missingClasses = new HashSet<>();
    public final Map<String, Class<?>> classCache = new HashMap<>();

    public final Set<String> missingResourceCache = new HashSet<>();
    public final Map<String, byte[]> resourceCache = new HashMap<>();

    public final Set<String> excludedClasses = new HashSet<>();
    public final Set<String> transformerExcludedClasses = new HashSet<>();

    public final List<IClassTransformer> transformers = new ArrayList<>();

    public PieceClassLoader() {
        this(new URL[0], ClassLoader.class.getClassLoader());
    }

    public PieceClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public PieceClassLoader(URL[] sources, ClassLoader parent) {
        super(sources, parent);
        this.sources.addAll(List.of(sources));

        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("com.sun.");
        addClassLoaderExclusion("javax.");
        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("org.apache.logging.");
        addClassLoaderExclusion("org.slf4j");
        addClassLoaderExclusion("com.google.");
        addClassLoaderExclusion("org.hjson.");

        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.fix.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.mod.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.util.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.launch.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.loading.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.provider.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.transformers.");

        addTransformerExclusion("javax.");
        addTransformerExclusion("argo.");
        addTransformerExclusion("com.google.common.");
        addTransformerExclusion("org.bouncycastle.");
    }


    public PieceClassLoader(List<URL> sources, ClassLoader parent) {
        this(sources.toArray(URL[]::new), parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
        this.sources.add(url);
    }

    public void addURL(URL... urls) {
        for (URL u : urls) addURL(u);
    }

    public void registerTransformer(IClassTransformer transformer) {
        transformers.add(transformer);
    }

    public void registerTransformer(String s) {
        try {
            registerTransformer((IClassTransformer) loadClass(s).newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Class transformer \"" + s + "\" failed, please report this to the creator of the transformer.");
        }
    }

    public void registerTransformers(String @NotNull ... transformerClassNames) {
        for (String transformerClassName : transformerClassNames) {
            registerTransformer(transformerClassName);
        }
    }

    public Class<?> defineClass(String clazzName, byte[] bytes) {
        return super.defineClass(clazzName, bytes, 0, bytes.length);
    }

//    @Override
//    public Class<?> loadClass(String name) throws ClassNotFoundException {
//        return findClass(name);
//    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (missingClasses.contains(name)) throw new ClassNotFoundException(name);

        for (String exclusion : excludedClasses) {
            if (name.startsWith(exclusion)) return parent.loadClass(name);
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
        if (!LoaderConstants.CLIConfiguration.TRANSFORMERS_ENABLED) return bytes;

        byte[] transformed = bytes;
        for (IClassTransformer transformer : transformers) {
            transformed = transformer.transform(fileName, name, transformed);
        }
        if (transformed != bytes) {
            BootstrapClassLoader.outputClass(name, transformed);
        }
        return transformed;
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

    public byte[] getResourceBytes(String name) {
        if (BootstrapClassLoader.overrides) {
            RawAssetLoader.RawFileHandle handle = RawAssetLoader.getLowLevelRelativeAssetErrors(BootstrapClassLoader.classOverridesDir, "/".concat(toFileName(name)), false);
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

    public void addClassLoaderExclusion(String s) {
        excludedClasses.add(s);
    }

    public void addTransformerExclusion(String s) {
        transformerExcludedClasses.add(s);
    }

    public List<ITransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    @Override
    public boolean isClassLoaded(String name) {
        return this.classCache.containsKey(name);
    }

    @Override
    public String getClassRestrictions(String className) {
        String restrictions = "";
        if (this.isClassClassLoaderExcluded(className, null)) {
            restrictions = "PACKAGE_CLASSLOADER_EXCLUSION";
        }
        if (this.isClassTransformerExcluded(className, null)) {
            restrictions = (!restrictions.isEmpty() ? restrictions + "," : "") + "PACKAGE_TRANSFORMER_EXCLUSION";
        }
        return restrictions;
    }

    public boolean isClassExcluded(String name, String transformedName) {
        return this.isClassClassLoaderExcluded(name, transformedName) || this.isClassTransformerExcluded(name, transformedName);
    }

    boolean isClassClassLoaderExcluded(String name, String transformedName) {
        for (final String exception : this.getClassLoaderExceptions()) {
            if ((transformedName != null && transformedName.startsWith(exception)) || name.startsWith(exception)) {
                return true;
            }
        }

        return false;
    }

    boolean isClassTransformerExcluded(String name, String transformedName) {
        for (final String exception : this.getTransformerExceptions()) {
            if ((transformedName != null && transformedName.startsWith(exception)) || name.startsWith(exception)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void registerInvalidClass(String name) {
        this.missingClasses.add(name);
    }

    Set<String> getClassLoaderExceptions() {
        return this.excludedClasses;
    }

    Set<String> getTransformerExceptions() {
        return this.transformerExcludedClasses;
    }

    public void usesOverrides(boolean overrides) {
        BootstrapClassLoader.overrides = overrides;
    }
}