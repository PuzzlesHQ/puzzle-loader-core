package dev.puzzleshq.puzzleloader.loader.launch;

import dev.puzzleshq.puzzleloader.loader.LoaderConfig;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.ITransformer;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Custom class loader for Puzzle Loader that handles class and resource loading,
 * class transformers, overrides, exclusions, and caching mechanisms.
 * <p>
 * Supports loading classes from URLs, applying legacy and custom transformers,
 * and dumping or overriding classes for debugging or runtime modifications.
 * </p>
 * <p>
 * Integrates with the Puzzle Loader environment and provides hooks for
 * mixins, mods, and game providers.
 * </p>
 */
public class PieceClassLoader extends URLClassLoader implements IClassTracker {

    /** Parent class loader */
    public final ClassLoader parent = getClass().getClassLoader();

    /** URLs of loaded sources */
    public final List<URL> sources = new ArrayList<>();

    /** Cache of classes that could not be found */
    public final Set<String> missingClasses = new HashSet<>();
    /** Cache of loaded classes */
    public final Map<String, Class<?>> classCache = new HashMap<>();

    /** Cache of missing resources */
    public final Set<String> missingResourceCache = new HashSet<>();
    /** Cache of loaded resources */
    public final Map<String, byte[]> resourceCache = new HashMap<>();

    /** Classes excluded from loading */
    public final Set<String> excludedClasses = new HashSet<>();
    /** Classes excluded from transformation */
    public final Set<String> transformerExcludedClasses = new HashSet<>();

    /** Registered class transformers */
    public final List<ILegacyClassTransformer> transformers = new ArrayList<>();

    /**
     * Creates a new PieceClassLoader with default parent.
     */
    public PieceClassLoader() {
        this(new URL[0], PieceClassLoader.class.getClassLoader());
    }

    public PieceClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public PieceClassLoader(Collection<URL> urls) {
        this(urls.toArray(new URL[0]), PieceClassLoader.class.getClassLoader());
    }

    public PieceClassLoader(URL[] sources, ClassLoader parent) {
        super(sources, parent);
        this.sources.addAll(Arrays.asList(sources));

        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("com.sun.");
        addClassLoaderExclusion("javax.");
        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("org.apache.logging.");
        addClassLoaderExclusion("org.slf4j");
        addClassLoaderExclusion("com.google.");
        addClassLoaderExclusion("org.hjson.");
        addClassLoaderExclusion("org.xml.");
        addClassLoaderExclusion("org.w3c.");
        addClassLoaderExclusion("org.objectweb.");

        addClassLoaderExclusion("dev.puzzleshq.mod.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.launch.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.util.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.LoaderConfig");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.LoaderConstants");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.loading.");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.provider.game.IPatchableGameProvider");
        addClassLoaderExclusion("dev.puzzleshq.puzzleloader.loader.transformers.");
        addClassLoaderExclusion("net.minecraft.launchwrapper.");
        addClassLoaderExclusion("org.spongepowered.");
        addClassLoaderExclusion("com.llamalad7.");

        addTransformerExclusion("javax.");
        addTransformerExclusion("argo.");
        addTransformerExclusion("com.google.common.");
        addTransformerExclusion("org.bouncycastle.");
    }

    public PieceClassLoader(List<URL> sources, ClassLoader parent) {
        this(sources.toArray(new URL[0]), parent);
    }

    /**
     * Loads system properties for class overrides and class dump options.
     */
    public static void loadSystemProperties() {
        LoaderConfig.ALLOWS_CLASS_OVERRIDES = overrides = Boolean.parseBoolean(System.getProperty("puzzle.core.classloader.classOverrides"));
        LoaderConfig.DUMP_TRANSFORMED_CLASSES = dumpClasses = Boolean.parseBoolean(System.getProperty("puzzle.core.classloader.classDump"));
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
        this.sources.add(url);
    }

    public void addURL(URL... urls) {
        for (URL u : urls) addURL(u);
    }

    /** Registers a legacy transformer instance */
    public void registerTransformer(ILegacyClassTransformer transformer) {
        transformers.add(transformer);
    }

    /** Registers a transformer by class name */
    public void registerTransformer(String s) {
        try {
            registerTransformer((ILegacyClassTransformer) loadClass(s).newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Class transformer \"" + s + "\" failed, please report this to the creator of the transformer.");
        }
    }

    /** Registers multiple transformers by class names */
    public void registerTransformers(String @NotNull ... transformerClassNames) {
        for (String transformerClassName : transformerClassNames) {
            registerTransformer(transformerClassName);
        }
    }

    /** Defines a class from byte array */
    public Class<?> defineClass(String clazzName, byte[] bytes) {
        return super.defineClass(clazzName, bytes, 0, bytes.length);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    private static Method I_HATE_YOU_JAVA_CLASSLOADERS;
    private static boolean aboveJava8;

    static {
        try {
            I_HATE_YOU_JAVA_CLASSLOADERS = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            I_HATE_YOU_JAVA_CLASSLOADERS.setAccessible(true);
        } catch (Exception e) {
            aboveJava8 = true;
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (missingClasses.contains(name)) throw new ClassNotFoundException(name);

        if (name.endsWith(".package-info")) return parent.loadClass(name);

        for (String exclusion : excludedClasses) {
            if (name.startsWith(exclusion)) return parent.loadClass(name);
        }

        if (!aboveJava8) {
            try {
                Class<?> parentTest = (Class<?>) I_HATE_YOU_JAVA_CLASSLOADERS.invoke(parent, name);
                if (parentTest != null) return parentTest;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
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
            String classFileNameSoft = name.replaceAll("\\.", "/") + ".class";
            String fileName = toFileName(name);

            URLConnection connection = getConnection(classFileNameSoft);
            CodeSigner[] signers = null;
            CodeSource source = null;

            if (connection instanceof JarURLConnection) {
                JarURLConnection jarConnection = (JarURLConnection) connection;
                JarFile jar = jarConnection.getJarFile();

                if (jar != null && jar.getManifest() != null) {
                    JarEntry entry = jar.getJarEntry(fileName);
                    getResourceBytes(name);
                    signers = entry.getCodeSigners();
                    source = new CodeSource(jarConnection.getJarFileURL(), signers);
                }
                if (lastDot > -1 && getPackage(pkgName) == null)
                    definePackage(pkgName, jarConnection.getManifest(), jarConnection.getJarFileURL());
            } else if (lastDot > -1 && getPackage(pkgName) == null) {
                definePackage(pkgName, null, null, null, null, null, null, null);
            }

            if (connection == null) {
                return parent.loadClass(name);
            }

            byte[] bytes = transform(name, name, getResourceBytes(name));
            if (bytes == null) {
                missingClasses.add(name);
                throw new ClassNotFoundException(name);
            }

            Class<?> clazz;
            try {
                clazz = defineClass(name, bytes, 0, bytes.length, source);
                classCache.put(name, clazz);
            } catch (LinkageError e) {
                if (e.getMessage().contains("previously loaded")) {
                    return Class.forName(name, false, parent);
                }
                throw e;
            }
            return clazz;
        } catch (IOException e) {
            missingClasses.add(name);
            throw new ClassNotFoundException(name, e);
        }
    }

    public static boolean overrides = false;
    public static boolean dumpClasses;

    /** Transforms class bytes using registered transformers */
    private byte[] transform(String name, String fileName, byte[] bytes) {
        if (!LoaderConfig.TRANSFORMERS_ENABLED) return bytes;

        byte[] transformed = bytes;
        for (ILegacyClassTransformer transformer : transformers) {
            transformed = transformer.transformClassBytes(fileName, name, transformed);
        }
        if (transformed != bytes) {
            outputClass(name, transformed);
        }
        return transformed;
    }

    public static final File classOverridesDir = new File(".class-overrides");
    public static final File classDumpDir = new File(".class-transform-dump");

    /** Outputs transformed class to disk if dumping is enabled */
    public static void outputClass(String name, byte[] transformed) {
        if (!dumpClasses) return;
        try {
            File output = new File(classDumpDir, name.replaceAll("\\.", "/") + ".class");
            if (!output.getParentFile().exists()) output.getParentFile().mkdirs();
            if (!output.exists()) output.createNewFile();
            try (FileOutputStream stream = new FileOutputStream(output)) {
                stream.write(transformed);
            }
        } catch (IOException ignore) {}
    }

    /** Returns the URL connection for a resource */
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

    /** Loads resource bytes from classpath or overrides */
    public byte[] getResourceBytes(String name) {
        if (PieceClassLoader.overrides) {
            if (!PieceClassLoader.classOverridesDir.exists()) PieceClassLoader.classOverridesDir.mkdirs();
            RawAssetLoader.RawFileHandle handle = RawAssetLoader.getLowLevelRelativeAssetErrors(PieceClassLoader.classOverridesDir, "/".concat(toFileName(name)), false);
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
}
