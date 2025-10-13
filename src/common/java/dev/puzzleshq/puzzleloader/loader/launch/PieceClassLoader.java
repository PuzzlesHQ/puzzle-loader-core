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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
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
 * Custom class loader responsible for managing modded or patched classes within the Puzzle Loader framework.
 * <p>
 * The {@code PieceClassLoader} extends {@link URLClassLoader} and integrates class transformation, caching,
 * exclusion management, and code source verification. It provides fine-grained control over how classes
 * and resources are discovered, transformed, and loaded into the runtime.
 * </p>
 */
public class PieceClassLoader extends URLClassLoader implements IClassTracker {

    /** The parent class loader used for delegation. */
    public final ClassLoader parent = getClass().getClassLoader();

    /** Registered source URLs used for class lookup. */
    public final Set<URL> sources = new HashSet<>();

    /** Names of classes that could not be found during loading. */
    public final Set<String> missingClasses = new HashSet<>();

    /** Cache of already loaded classes. */
    public final Map<String, Class<?>> classCache = new HashMap<>();

    /** Cache of missing resources. */
    public final Set<String> missingResourceCache = new HashSet<>();

    /** Cache of loaded resource byte arrays. */
    public final Map<String, byte[]> resourceCache = new HashMap<>();

    /** Class name prefixes excluded from this class loader’s control. */
    public final Set<String> excludedClasses = new HashSet<>();

    /** Class name prefixes excluded from bytecode transformation. */
    public final Set<String> transformerExcludedClasses = new HashSet<>();

    /** Registered class transformers. */
    public final List<ILegacyClassTransformer> transformers = new ArrayList<>();

    /** Default constructor using the system class loader as parent. */
    public PieceClassLoader() {
        this(new URL[0], PieceClassLoader.class.getClassLoader());
    }

    /** Constructs a new loader with a specific parent. */
    public PieceClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    /** Constructs a new loader from a list of URLs. */
    public PieceClassLoader(Collection<URL> urls) {
        this(urls.toArray(new URL[0]), PieceClassLoader.class.getClassLoader());
    }

    /**
     * Main constructor initializing sources, exclusions, and transformer exclusions.
     *
     * @param sources array of initial source URLs
     * @param parent  parent class loader
     */
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

    /** Constructs a new loader from a list of URLs and a specific parent. */
    public PieceClassLoader(List<URL> sources, ClassLoader parent) {
        this(sources.toArray(new URL[0]), parent);
    }

    /** Loads system properties affecting class loader behavior. */
    public static void loadSystemProperties() {
        LoaderConfig.ALLOWS_CLASS_OVERRIDES = overrides = Boolean.parseBoolean(System.getProperty("puzzle.core.classloader.classOverrides"));
        LoaderConfig.DUMP_TRANSFORMED_CLASSES = dumpClasses = Boolean.parseBoolean(System.getProperty("puzzle.core.classloader.classDump"));
    }

    /** Adds a single URL source. */
    @Override
    public void addURL(URL url) {
        super.addURL(url);
        this.sources.add(url);
    }

    /** Adds multiple URL sources. */
    public void addURL(URL... urls) {
        for (URL u : urls) addURL(u);
    }

    /** Registers a transformer instance. */
    public void registerTransformer(ILegacyClassTransformer transformer) {
        transformers.add(transformer);
    }

    /** Instantiates and registers a transformer by class name. */
    public void registerTransformer(String s) {
        try {
            registerTransformer((ILegacyClassTransformer) loadClass(s).newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Class transformer \"" + s + "\" failed, please report this to the creator of the transformer.");
        }
    }

    /** Registers multiple transformer classes by name. */
    public void registerTransformers(String @NotNull ... transformerClassNames) {
        for (String transformerClassName : transformerClassNames) {
            registerTransformer(transformerClassName);
        }
    }

    /** Defines a class from raw bytecode. */
    public Class<?> defineClass(String clazzName, byte[] bytes) {
        return super.defineClass(clazzName, bytes, 0, bytes.length);
    }

    /** Overrides default class loading behavior to use custom lookup logic. */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    /** Reflection handle to bypass internal loader restrictions. */
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

    /** Core class lookup, caching, transformation, and code source validation routine. */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (missingClasses.contains(name)) throw new ClassNotFoundException(name);

        if (name.endsWith(".package-info")) return parent.loadClass(name);

        for (String exclusion : excludedClasses) {
            if (name.startsWith(exclusion)) return parent.loadClass(name);
        }

        if (aboveJava8){

        } else {
            Class<?> parentTest;
            try {
                parentTest = (Class<?>) I_HATE_YOU_JAVA_CLASSLOADERS.invoke(parent, name);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (parentTest != null) return parentTest;
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
                JarURLConnection  jarConnection = (JarURLConnection) connection;

                JarFile jar = jarConnection.getJarFile();

                if (jar != null && jar.getManifest() != null) {
                    JarEntry entry = jar.getJarEntry(fileName);

                    getResourceBytes(name);
                    signers = entry.getCodeSigners();
                    source = new CodeSource(jarConnection.getJarFileURL(), signers);
                }
                if (lastDot > -1) {
                    if (getPackage(pkgName) == null)
                        definePackage(pkgName, jarConnection.getManifest(), jarConnection.getJarFileURL());
                }
            } else {
                if (lastDot > -1) {
                    Package pkg = getPackage(pkgName);
                    if (pkg == null)
                        definePackage(pkgName, null, null, null, null, null, null, null);
                }
            }

            if (connection == null) {
                try {
                    return parent.loadClass(name);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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

    /** Whether class overrides and dumps are enabled. */
    public static boolean overrides = false;
    public static boolean dumpClasses;

    /** Applies all registered class transformers to a class byte array. */
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

    /** Directories for overridden and dumped classes. */
    public static final File classOverridesDir = new File(".class-overrides");
    public static final File classDumpDir = new File(".class-transform-dump");

    /** Writes transformed class bytecode to disk if dumping is enabled. */
    public static void outputClass(String name, byte[] transformed) {
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

    /** Opens a URL connection for a resource path. */
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

    /** Loads class resource bytes from file overrides or classpath. */
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

    /** Converts a class name into its corresponding file path. */
    private String toFileName(String name) {
        return name.replace('.', '/').concat(".class");
    }

    /** Adds a prefix to the class loader exclusion list. */
    public void addClassLoaderExclusion(String s) {
        excludedClasses.add(s);
    }

    /** Adds a prefix to the transformer exclusion list. */
    public void addTransformerExclusion(String s) {
        transformerExcludedClasses.add(s);
    }

    /** Returns a read-only list of registered transformers. */
    public List<ITransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    /** Returns whether a class is already loaded and cached. */
    @Override
    public boolean isClassLoaded(String name) {
        return this.classCache.containsKey(name);
    }

    /** Describes class restrictions based on exclusion categories. */
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

    /** Determines if a class is excluded from both loading and transformation. */
    public boolean isClassExcluded(String name, String transformedName) {
        return this.isClassClassLoaderExcluded(name, transformedName) || this.isClassTransformerExcluded(name, transformedName);
    }

    /** Checks if a class is excluded from this loader. */
    boolean isClassClassLoaderExcluded(String name, String transformedName) {
        for (final String exception : this.getClassLoaderExceptions()) {
            if ((transformedName != null && transformedName.startsWith(exception)) || name.startsWith(exception)) {
                return true;
            }
        }
        return false;
    }

    /** Checks if a class is excluded from transformation. */
    boolean isClassTransformerExcluded(String name, String transformedName) {
        for (final String exception : this.getTransformerExceptions()) {
            if ((transformedName != null && transformedName.startsWith(exception)) || name.startsWith(exception)) {
                return true;
            }
        }
        return false;
    }

    /** Registers a class as invalid or unloadable. */
    @Override
    public void registerInvalidClass(String name) {
        this.missingClasses.add(name);
    }

    /** Returns the current class loader exclusion set. */
    Set<String> getClassLoaderExceptions() {
        return this.excludedClasses;
    }

    /** Returns the current transformer exclusion set. */
    Set<String> getTransformerExceptions() {
        return this.transformerExcludedClasses;
    }
}
