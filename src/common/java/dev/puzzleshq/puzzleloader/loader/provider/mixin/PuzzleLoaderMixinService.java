/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package dev.puzzleshq.puzzleloader.loader.provider.mixin;

import org.objectweb.asm.tree.ClassNode;
import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.provider.mixin.transformers.BetterProxy;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.service.modlauncher.LoggerAdapterLog4j2;
import org.spongepowered.asm.transformers.MixinClassReader;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Mixin service for launchwrapper
 */
public class PuzzleLoaderMixinService implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider {
    protected static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
    private static final String TRANSFORMER_PROXY_CLASS = BetterProxy.class.getName();
    protected final ReEntranceLock lock = new ReEntranceLock(1);
    private final Map<Class<IMixinInternal>, IMixinInternal> internals = new HashMap();
    private List<IMixinPlatformServiceAgent> serviceAgents;

    /**
     * Known re-entrant transformers, other re-entrant transformers will
     * detect automatically
     */
    private static final Set<String> excludeTransformers = new HashSet<>();

    /**
     * Log4j2 logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("Puzzle | MixinService");

    /**
     * Local transformer chain, this consists of all transformers present at the
     * init phase with the exclusion of the mixin transformer itself and known
     * re-entrant transformers. Detected re-entrant transformers will be
     * subsequently removed.
     */
    private List<ILegacyClassTransformer> delegatedTransformers;

    /**
     * Class name transformer (if present)
     */
    public PuzzleLoaderMixinService() {
    }

    @Override
    public String getName() {
        return "Puzzle Mixin Service";
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isValid()
     */
    @Override
    public boolean isValid() {
        try {
            // Detect launchwrapper
            Piece.classLoader.hashCode();
        } catch (Throwable ex) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#prepare()
     */
    @Override
    public void prepare() {
        // Only needed in dev, in production this would be handled by the tweaker
        Piece.classLoader.addClassLoaderExclusion(LAUNCH_PACKAGE);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getInitialPhase()
     */
    @Override
    public Phase getInitialPhase() {
        String command = System.getProperty("sun.java.command");
        if (command != null && command.contains("GradleStart")) {
            System.setProperty("mixin.env.remapRefMap", "true");
        }

        return Phase.PREINIT;
    }

    @Override
    public void offer(IMixinInternal iMixinInternal) {

    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #getMaxCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMaxCompatibilityLevel() {
        return CompatibilityLevel.JAVA_6;
    }

    @Override
    public ILogger getLogger(String s) {
        return new LoggerAdapterLog4j2(getName());
    }

    private List<IMixinPlatformServiceAgent> getServiceAgents() {
        if (this.serviceAgents == null) {
            this.serviceAgents = new ArrayList<>();

            for (String agentClassName : this.getPlatformAgents()) {
                try {
                    Class<IMixinPlatformAgent> agentClass = (Class<IMixinPlatformAgent>) this.getClassProvider().findClass(agentClassName, false);
                    IMixinPlatformAgent agent = agentClass.getDeclaredConstructor().newInstance();
                    if (agent instanceof IMixinPlatformServiceAgent) {
                        this.serviceAgents.add((IMixinPlatformServiceAgent) agent);
                    }
                } catch (Exception var5) {
                    var5.printStackTrace();
                }
            }

        }
        return this.serviceAgents;
    }

    @Override
    public void init() {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.init();
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getPlatformAgents()
     */
    @Override
    public Collection<String> getPlatformAgents() {
        return new ArrayList<>();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual(this.getName());
//        try {
//            URI uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
//            if (uri != null) {
//                return new ContainerHandleURI(uri);
//            }
//        } catch (URISyntaxException ex) {
//            ex.printStackTrace();
//        }
//        return new ContainerHandleVirtual(this.getName());
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        List<IContainerHandle> list = new ArrayList<>();
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            Collection<IContainerHandle> containers = agent.getMixinContainers();
            if (containers != null) {
                list.addAll(containers);
            }
        }
        return list;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassProvider()
     */
    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getBytecodeProvider()
     */
    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformerProvider()
     */
    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassTracker()
     */
    @Override
    public IClassTracker getClassTracker() {
        return Piece.classLoader;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getAuditTrail()
     */
    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findClass(
     *      java.lang.String)
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Piece.classLoader.findClass(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findClass(
     *      java.lang.String, boolean)
     */
    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Piece.classLoader);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findAgentClass(
     *      java.lang.String, boolean)
     */
    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Piece.class.getClassLoader());
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#beginPhase()
     */
    @Override
    public void beginPhase() {
        Piece.classLoader.registerTransformer(PuzzleLoaderMixinService.TRANSFORMER_PROXY_CLASS);
        this.delegatedTransformers = null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#checkEnv(
     *      java.lang.Object)
     */
    @Override
    public void checkEnv(@NotNull Object bootSource) {
        if (bootSource.getClass().getClassLoader() != Piece.class.getClassLoader()) {
            throw new MixinException("Attempted to init the mixin environment in the wrong classloader");
        }
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return lock;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getResourceAsStream(
     *      java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        return Piece.classLoader.getResourceAsStream(name);
    }

    @Override
    public String getSideName() {
        return LoaderConstants.SIDE.name;
    }

    @Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return CompatibilityLevel.JAVA_22;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#getClassPath()
     */
    @Override
    @Deprecated
    public URL[] getClassPath() {
        return Piece.classLoader.sources.toArray(new URL[0]);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformers()
     */
    @Override
    public Collection<ITransformer> getTransformers() {
        return Piece.classLoader.getTransformers();
    }

    /**
     * Returns (and generates if necessary) the transformer delegation list for
     * this environment.
     *
     * @return current transformer delegation list (read-only)
     */
    @Override
    public List<ITransformer> getDelegatedTransformers() {
        return Collections.<ITransformer>unmodifiableList(this.getDelegatedLegacyTransformers());
    }

    private List<ILegacyClassTransformer> getDelegatedLegacyTransformers() {
        if (this.delegatedTransformers == null) {
            this.buildTransformerDelegationList();
        }

        return this.delegatedTransformers;
    }

    /**
     * Builds the transformer list to apply to loaded mixin bytecode. Since
     * generating this list requires inspecting each transformer by name (to
     * cope with the new wrapper functionality added by FML) we generate the
     * list just once per environment and cache the result.
     */
    private void buildTransformerDelegationList() {
        PuzzleLoaderMixinService.LOGGER.debug("Rebuilding transformer delegation list:");
        this.delegatedTransformers = new ArrayList<ILegacyClassTransformer>();
        for (ITransformer transformer : this.getTransformers()) {
            if (!(transformer instanceof ILegacyClassTransformer)) {
                continue;
            }

            ILegacyClassTransformer legacyTransformer = (ILegacyClassTransformer)transformer;
            String transformerName = legacyTransformer.getName();
            boolean include = true;
            for (String excludeClass : PuzzleLoaderMixinService.excludeTransformers) {
                if (transformerName.contains(excludeClass)) {
                    include = false;
                    break;
                }
            }
            if (include && !legacyTransformer.isDelegationExcluded()) {
                PuzzleLoaderMixinService.LOGGER.debug("  Adding:    {}", transformerName);
                this.delegatedTransformers.add(legacyTransformer);
            } else {
                PuzzleLoaderMixinService.LOGGER.debug("  Excluding: {}", transformerName);
            }
        }

        PuzzleLoaderMixinService.LOGGER.debug("Transformer delegation list created with {} entries", this.delegatedTransformers.size());
    }

    /**
     * Adds a transformer to the transformer exclusions list
     *
     * @param name Class transformer exclusion to add
     */
    @Override
    public void addTransformerExclusion(String name) {
        PuzzleLoaderMixinService.excludeTransformers.add(name);

        // Force rebuild of the list
        this.delegatedTransformers = null;
    }

    /**
     * Retrieve class bytes using available classloaders, does not transform the
     * class
     *
     * @param name class name
     * @param transformedName transformed class name
     * @return class bytes or null if not found
     * @throws IOException propagated
     * @deprecated Use {@link #getClassNode} instead
     */
    @Deprecated
    public byte[] getClassBytes(String name, String transformedName) throws IOException {
        byte[] classBytes = Piece.classLoader.getResourceBytes(name);
        if (classBytes != null) {
            return classBytes;
        }

        URLClassLoader appClassLoader;
        if (Piece.class.getClassLoader() instanceof URLClassLoader) {
            appClassLoader = (URLClassLoader) Piece.class.getClassLoader();
        } else {
            appClassLoader = new URLClassLoader(new URL[]{}, Piece.class.getClassLoader());
        }

        InputStream classStream = null;
        try {
            final String resourcePath = transformedName.replace('.', '/').concat(".class");
            classStream = appClassLoader.getResourceAsStream(resourcePath);
            assert classStream != null;
            return classStream.readAllBytes();
        } catch (Exception ex) {
            return null;
        } finally {
            classStream.close();
        }
    }

    /**
     * Loads class bytecode from the classpath
     *
     * @param className Name of the class to load
     * @param runTransformers True to run the loaded bytecode through the
     *      delegate transformer chain
     * @return Transformed class bytecode for the specified class
     * @throws ClassNotFoundException if the specified class could not be loaded
     * @throws IOException if an error occurs whilst reading the specified class
     */
    @Deprecated
    public byte[] getClassBytes(@NotNull String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        String transformedName = className.replace('/', '.');
        String name = transformedName;

        Profiler profiler = Profiler.getProfiler("mixin");
        Section loadTime = profiler.begin(Profiler.ROOT, "class.load");
        byte[] classBytes = this.getClassBytes(name, transformedName);
        loadTime.end();

        if (runTransformers) {
            Section transformTime = profiler.begin(Profiler.ROOT, "class.transform");
            classBytes = this.applyTransformers(name, transformedName, classBytes, profiler);
            transformTime.end();
        }

        if (classBytes == null) {
            throw new ClassNotFoundException(String.format("The specified class '%s' was not found", transformedName));
        }

        return classBytes;
    }

    /**
     * Since we obtain the class bytes with getClassBytes(), we need to apply
     * the transformers ourselves
     *
     * @param name class name
     * @param transformedName transformed class name
     * @param basicClass input class bytes
     * @return class bytecode after processing by all registered transformers
     *      except the excluded transformers
     */
    private byte[] applyTransformers(String name, String transformedName, byte[] basicClass, Profiler profiler) {
        if (Piece.classLoader.isClassExcluded(name, transformedName)) {
            return basicClass;
        }

        for (ILegacyClassTransformer transformer : this.getDelegatedLegacyTransformers()) {
            // Clear the re-entrance semaphore
            this.lock.clear();

            int pos = transformer.getName().lastIndexOf('.');
            String simpleName = transformer.getName().substring(pos + 1);
            Section transformTime = profiler.begin(Profiler.FINE, simpleName.toLowerCase(Locale.ROOT));
            transformTime.setInfo(transformer.getName());
            basicClass = transformer.transformClassBytes(name, transformedName, basicClass);
            transformTime.end();

            if (this.lock.isSet()) {
                // Also add it to the exclusion list so we can exclude it if the environment triggers a rebuild
                this.addTransformerExclusion(transformer.getName());

                this.lock.clear();
                PuzzleLoaderMixinService.LOGGER.info("A re-entrant transformer '{}' was detected and will no longer process meta class data",
                        transformer.getName());
            }
        }

        return basicClass;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassNode(
     *      java.lang.String)
     */
    @Override
    public ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return this.getClassNode(className, this.getClassBytes(className, true), ClassReader.EXPAND_FRAMES);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassBytecodeProvider#getClassNode(
     *      java.lang.String, boolean)
     */
    @Override
    public ClassNode getClassNode(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        return this.getClassNode(className, this.getClassBytes(className, runTransformers), ClassReader.EXPAND_FRAMES);
    }

    @Override
    public ClassNode getClassNode(String className, boolean runTransformers, int flags) throws ClassNotFoundException, IOException {
        return getClassNode(className, getClassBytes(className, runTransformers), flags);
    }

    /**
     * Gets an ASM Tree for the supplied class bytecode
     *
     * @param classBytes Class bytecode
     * @param flags ClassReader flags
     * @return ASM Tree view of the specified class
     */
    private @NotNull ClassNode getClassNode(String className, byte[] classBytes, int flags) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new MixinClassReader(classBytes, className);
        classReader.accept(classNode, flags);
        return classNode;
    }
}