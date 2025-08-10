package dev.puzzleshq.puzzleloader.loader.provider.mixin.service;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.FlexPiece;
import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapPiece;
import dev.puzzleshq.puzzleloader.loader.provider.mixin.transformers.BetterProxy;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.ReEntranceLock;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FlexMixinService implements IMixinService {

    MixinEnvironment.CompatibilityLevel MAX_LEVEL = MixinEnvironment.CompatibilityLevel.JAVA_22;
    MixinEnvironment.CompatibilityLevel MIN_LEVEL = MixinEnvironment.CompatibilityLevel.JAVA_6;

    ReEntranceLock reEntranceLock = new ReEntranceLock(10);

    List<IContainerHandle> mixinContainers = new ArrayList<>();
    List<IMixinPlatformServiceAgent> serviceAgents;

    public FlexMixinService() {
    }

    private List<IMixinPlatformServiceAgent> getMixinPlatformAgents() {
        if (this.serviceAgents != null) return serviceAgents;
        this.serviceAgents = new ArrayList<>();

        for (String agent : getPlatformAgents()) {
            try {
                @SuppressWarnings("unchecked")
                Class<IMixinPlatformAgent> agentClass = (Class<IMixinPlatformAgent>) getClassProvider().findClass(agent, false);
                IMixinPlatformAgent instance = agentClass.getConstructor().newInstance();
                if (instance instanceof IMixinPlatformServiceAgent serviceInstance)
                    this.serviceAgents.add(serviceInstance);
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        return this.serviceAgents;
    }

    @Override
    public String getName() {
        return "FlexMixinService";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void prepare() {
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        System.setProperty("mixin.env.remapRefMap", "true");
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void offer(IMixinInternal internal) {
    }

    @Override
    public void init() {
        for (IMixinPlatformServiceAgent mixinPlatformAgent : getMixinPlatformAgents()) {
            mixinPlatformAgent.init();
        }
    }

    @Override
    public void beginPhase() {
        BootstrapPiece.generalClassloader.registerTransformer("dev.puzzleshq.puzzleloader.loader.provider.mixin.transformers.BetterProxy");
    }

    @Override
    public void checkEnv(Object bootSource) {

    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return reEntranceLock;
    }

    @Override
    public IClassProvider getClassProvider() {
        return BootstrapPiece.generalClassloader;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return BootstrapPiece.generalClassloader;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return BootstrapPiece.generalClassloader;
    }

    @Override
    public IClassTracker getClassTracker() {
        return BootstrapPiece.generalClassloader;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return new FlexMixinAuditTrail();
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return List.of();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        try {
            URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
            return new ContainerHandleURI(url.toURI());
        } catch (URISyntaxException e) {
            return new ContainerHandleVirtual(getClass().getName());
        }
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        mixinContainers.clear();
        for (IMixinPlatformServiceAgent mixinPlatformAgent : getMixinPlatformAgents()) {
            for (IContainerHandle mixinContainer : mixinPlatformAgent.getMixinContainers()) {
                mixinContainers.add(mixinContainer);
                System.out.println(mixinContainer);
            }
        }
        return mixinContainers;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return BootstrapPiece.generalClassloader.getResourceAsStream(name);
    }

    @Override
    public String getSideName() {
        return BootstrapPiece.ENV;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MAX_LEVEL;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MIN_LEVEL;
    }

    @Override
    public ILogger getLogger(String name) {
        return new LoggerAdapterConsole(name);
    }
}
