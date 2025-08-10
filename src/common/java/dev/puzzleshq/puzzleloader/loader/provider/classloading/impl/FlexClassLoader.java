package dev.puzzleshq.puzzleloader.loader.provider.classloading.impl;

import dev.puzzleshq.puzzleloader.loader.launch.FlexPiece;
import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapPiece;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.FlexClassPermissionController;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.FlexClassRestriction;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.FlexGlobalCLSettings;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.IFlexClassloader;
import dev.puzzleshq.puzzleloader.loader.util.FileCollection;
import dev.puzzleshq.puzzleloader.loader.util.URLCollection;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.jar.JarEntry;

public class FlexClassLoader extends URLClassLoader implements IFlexClassloader {

    public static final String DEFAULT_CLASS_LOADER_NAME = "TransformClassLoader";
    public final String name;
    public final List<URL> classPath;

    private FlexClassPermissionController controller;

    private final List<ITransformer> transformers = new ArrayList<>();

    public FlexClassLoader(URL[] urls, ClassLoader parent) {
        this(DEFAULT_CLASS_LOADER_NAME, urls, parent, null);
    }

    public FlexClassLoader(URL[] urls) {
        this(DEFAULT_CLASS_LOADER_NAME, urls, null, null);
    }

    public FlexClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        this(DEFAULT_CLASS_LOADER_NAME, urls, parent, factory);
    }

    public FlexClassLoader(String name, URL[] urls, ClassLoader parent) {
        this(name, urls, parent, null);
    }

    public FlexClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(name, urls, parent, factory);

        this.name = name;
        this.classPath = new ArrayList<>(List.of(urls));
        this.controller = new FlexClassPermissionController();

        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "java.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "com.sun.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "javax.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "sun.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.apache.logging.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.slf4j.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "com.google.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.objectweb.");

        registerRestriction(FlexClassRestriction.TRANSFORM_RESTRICTION, "javax.");
        registerRestriction(FlexClassRestriction.TRANSFORM_RESTRICTION, "argo.");
        registerRestriction(FlexClassRestriction.TRANSFORM_RESTRICTION, "com.google.common.");
        registerRestriction(FlexClassRestriction.TRANSFORM_RESTRICTION, "org.bouncycastle.");

        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "dev.puzzleshq.puzzleloader.loader.launch.bootstrap.");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.asm.service.ILegacyClassTransformer");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.asm.service.ITransformer");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.asm.service.IClassTracker");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.asm.service.IClassTracker");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.asm.service.IClassProvider");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.asm.service.IClassBytecode");
        registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "dev.puzzleshq.puzzleloader.loader.provider.classloading.");

        FlexGlobalCLSettings.loadRegistrationRestrictions(this);
    }

    List<FlexClassLoader> children = new ArrayList<>();
    public void addChild(FlexClassLoader classLoader) {
        children.add(classLoader);
    }

    public void registerTransformer(ILegacyClassTransformer transformer) {
        this.transformers.add(transformer);
    }

    @Override
    public String getName() {
        return name;
    }

    public void registerTransformer(String transformerClassName) {
        FlexClassRestriction.TRANSFORM_RESTRICTION.isRestricted("General-FlexClassLoader", transformerClassName);
        try {
            Class<ILegacyClassTransformer> transformerClass = (Class<ILegacyClassTransformer>) findClass(transformerClassName);
            registerTransformer(transformerClass.getConstructor().newInstance());
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public FlexClassPermissionController getPermissionController() {
        return controller;
    }

    public void setPermissionController(FlexClassPermissionController controller) {
        this.controller = controller;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
        this.classPath.add(url);
    }

    public void addURL(URL... urls) {
        for (URL url : urls) addURL(url);
    }

    public void addURL(URLCollection urls) {
        for (URL url : urls) addURL(url);
    }

    public void addURL(File file) {
        try {
            addURL(file.getAbsoluteFile().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addURL(File... urls) {
        for (File url : urls) addURL(url);
    }

    public void addURL(FileCollection urls) {
        for (File url : urls) addURL(url);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    @Override
    public URL[] getClassPath() {
        return classPath.toArray(new URL[0]);
    }

    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        for (FlexClassLoader child : children) {
            if (child.isClassLoaded(className)) return child.findClass(className);
        }
        Class<?> clazz = findClass0(className);
        return clazz;
    }

    public Class<?> findClass0(String className) throws ClassNotFoundException {
        try {
            if (Objects.equals(className, "dev.puzzleshq.puzzleloader.loader.provider.classloading.impl.FlexClassLoader"))
                return getParent().loadClass(className);

            if (invalidClasses.contains(className)) return null;

            boolean shouldUseParentLoader = FlexClassRestriction.CLASS_LOADING_RESTRICTION.isRestricted(name, className);
            if (shouldUseParentLoader) {
                return getParent().loadClass(className);
            }
            if (isClassLoaded(className)) {
                return loadedClasses.get(className);
            }

            int dot = className.lastIndexOf('.');
            String pkgName = dot == -1 ? null : className.substring(0, dot);
            String classFileNameHard = ("/" + className).replaceAll("\\.", "/") + ".class";
            String classFileNameSoft = ("/" + className).replaceAll("\\.", "/") + ".class";
            URL resource = getResource(classFileNameHard);
            if (resource == null) {
                classFileNameHard = className.replaceAll("\\.", "/") + ".class";
                resource = getResource(classFileNameHard);
            }
            if (resource == null)
                throw new ClassNotFoundException(name + ": " + className + ", null resource. " + classFileNameSoft);

            InputStream stream;
            if (FlexGlobalCLSettings.ALLOW_CLASS_OVERRIDING.get()) {
                try {
                    stream = FlexGlobalCLSettings.getOverride(classFileNameHard).openStream();
                } catch (IOException e) {
                    stream = resource.openStream();
                }
            } else {
                stream = resource.openStream();
            }
            byte[] bytes = stream.readAllBytes();
            stream.close();

            URLConnection connection = resource.openConnection();
            if (connection instanceof JarURLConnection jarURLConnection) {
                JarEntry entry = jarURLConnection.getJarEntry();

                Certificate[] certificates = entry.getCertificates();
                CodeSigner[] signers = entry.getCodeSigners();

                CodeSource source = null;
                if (certificates != null && certificates.length != 0) {
                    source = new CodeSource(jarURLConnection.getJarFileURL(), certificates);
                    CodeSigner[] signers1 = source.getCodeSigners();
                    if (signers != null && signers.length != 0) {
                        CodeSigner[] codeSigners = new CodeSigner[signers1.length + signers.length];
                        System.arraycopy(signers1, 0, codeSigners, 0, signers1.length);
                        System.arraycopy(signers, 0, codeSigners, signers1.length, signers.length);
                        source = new CodeSource(jarURLConnection.getJarFileURL(), codeSigners);
                    }
                } else {
                    source = new CodeSource(jarURLConnection.getJarFileURL(), signers);
                }

                if (dot != -1) {
                    Package pkg = getPackage(pkgName);
                    if (pkg == null) {
                        pkg = definePackage(
                                pkgName,
                                jarURLConnection.getManifest(),
                                jarURLConnection.getJarFileURL()
                        );
                    }
                }

                PermissionCollection permissionCollection = this.controller.getPermissions(name);
                ProtectionDomain protectionDomain = new ProtectionDomain(
                        source, permissionCollection, this, new Principal[0]
                );

                if (!FlexClassRestriction.TRANSFORM_RESTRICTION.isRestricted(name, className)) {
                    bytes = transformClass(bytes, className, classFileNameSoft);
                }
                Class clazz = defineClass(className, bytes, 0, bytes.length, protectionDomain);
                loadedClasses.put(className, clazz);
                return clazz;
            } else {
                return super.findClass(name);
            }
        } catch (ClassNotFoundException exception) {
            invalidClasses.add(className);
            throw exception;
        } catch (IOException e) {
            invalidClasses.add(className);
            e.printStackTrace();
            throw new ClassNotFoundException(name + ": " + className);
        }
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, this);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, getParent());
    }

    public byte[] transformClass(byte[] bytes, String name, String file) {
        byte[] originalBytes = bytes.clone();
        for (ITransformer transformer : getTransformers()) {
            if (transformer instanceof ILegacyClassTransformer)
                bytes = ((ILegacyClassTransformer) transformer).transformClassBytes(name, file, bytes);
        }
        if (FlexGlobalCLSettings.DUMP_TRANSFORMED_CLASSES.get() && originalBytes != bytes) {
            FlexGlobalCLSettings.dumpClass(file, bytes);
        }
        return bytes;
    }

    public void registerRestriction(FlexClassRestriction restriction, String pkg) {
        restriction.add(name, pkg);
    }

    public final Set<String> invalidClasses = new HashSet<>();

    @Override
    public void registerInvalidClass(String className) {
        invalidClasses.add(className);
    }

    public final Map<String, Class<?>> loadedClasses = new HashMap<>();

    @Override
    public boolean isClassLoaded(String className) {
        return loadedClasses.containsKey(className);
    }

    /**
     * {@link IClassTracker#getClassRestrictions(String)}
     *
     * Possible restriction values {@link FlexClassRestriction}
     */
    @Override
    public String getClassRestrictions(String className) {
        return FlexClassRestriction.getRestrictions(name, className);
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        return transformers;
    }

    private static final Set<String> excludeTransformers = new HashSet<>();
    private List<ITransformer> delegatedTransformers;
    private void buildTransformerDelegationList() {
        this.delegatedTransformers = new ArrayList<ITransformer>();
        for (ITransformer transformer : this.getTransformers()) {
            if (!(transformer instanceof ILegacyClassTransformer)) {
                continue;
            }

            ITransformer legacyTransformer = transformer;
            String transformerName = legacyTransformer.getName();
            boolean include = true;
            for (String excludeClass : excludeTransformers) {
                if (transformerName.contains(excludeClass)) {
                    include = false;
                    break;
                }
            }
            if (include && !legacyTransformer.isDelegationExcluded()) {
                this.delegatedTransformers.add(legacyTransformer);
            } else {
            }
        }
    }

    private List<ITransformer> getDelegatedLegacyTransformers() {
        if (this.delegatedTransformers == null) {
            this.buildTransformerDelegationList();
        }

        return this.delegatedTransformers;
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers() {
        return getDelegatedLegacyTransformers();
    }

    @Override
    public void addTransformerExclusion(String name) {
        registerRestriction(FlexClassRestriction.TRANSFORM_RESTRICTION, name);
    }

    public byte[] getClassBytes(String name) throws ClassNotFoundException {
        for (FlexClassLoader child : children) {
            if (child.isClassLoaded(name)) return child.getClassBytes(name);
        }
        String classFileName = "/" + name.replaceAll("\\.", "/") + ".class";
        String classFileName2 = name.replaceAll("\\.", "/") + ".class";
        URL resource = findResource(classFileName);
        if (resource == null)
            resource = findResource(classFileName2);
        if (resource != null) {
            try {
                InputStream stream = resource.openStream();
                byte[] bytes = stream.readAllBytes();
                stream.close();

                return bytes;
            } catch (IOException e) {
                throw new ClassNotFoundException(name);
            }
        } else {
            try {
                InputStream stream = BootstrapPiece.generalClassloader.getResourceAsStream(classFileName2);
                byte[] bytes = stream.readAllBytes();
                stream.close();

                return bytes;
            } catch (IOException e) {
                throw new ClassNotFoundException(name);
            }
        }
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
        String classFileName = "/" + name.replaceAll("\\.", "/") + ".class";
        byte[] bytes = getClassBytes(name);
        if (runTransformers) bytes = transformClass(bytes, name, classFileName);

        ClassReader reader = new ClassReader(bytes, 0, bytes.length);
        ClassNode node = new ClassNode(Opcodes.ASM9);
        reader.accept(node, readerFlags);
        return node;
    }

    @Override
    public String toString() {
        return name;
    }
}
