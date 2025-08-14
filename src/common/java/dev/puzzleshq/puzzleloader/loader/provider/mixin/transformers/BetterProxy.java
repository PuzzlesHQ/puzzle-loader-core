package dev.puzzleshq.puzzleloader.loader.provider.mixin.transformers;

import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;
import dev.puzzleshq.puzzleloader.loader.util.ReflectionUtil;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class BetterProxy implements IClassTransformer, ILegacyClassTransformer {

    /**
     * All existing proxies
     */
    private static List<BetterProxy> proxies = new ArrayList<>();
    
    /**
     * Actual mixin transformer instance
     */
//    private static MixinTransformer transformer = new MixinTransformer();
    private static Object transformerO;
    private static Method transformClassBytes;
    
    /**
     * True if this is the active proxy, newer proxies disable their older
     * siblings
     */
    private boolean isActive = true;

    static {
        try {
            Class<?> transformer = MixinService.getService().getClassProvider().findAgentClass("org.spongepowered.asm.mixin.transformer.MixinTransformer", true);
            Constructor<?> constructor = transformer.getDeclaredConstructor();
            constructor.setAccessible(true);

            transformerO = constructor.newInstance();

            transformClassBytes = ReflectionUtil.getMethod(transformer, "transformClassBytes", String.class, String.class, byte[].class);
            transformClassBytes.setAccessible(true);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | ClassNotFoundException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public BetterProxy() {
        for (BetterProxy proxy : BetterProxy.proxies) {
            proxy.isActive = false;
        }
        
        BetterProxy.proxies.add(this);
        MixinService.getService().getLogger("mixin").debug("Adding new mixin transformer proxy #{}", BetterProxy.proxies.size());
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            try {
                return (byte[]) transformClassBytes.invoke(transformerO, name, transformedName, basicClass);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
//            return BetterProxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }

        return basicClass;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            try {
                return (byte[]) transformClassBytes.invoke(transformerO, name, transformedName, basicClass);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
//            return BetterProxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }
        
        return basicClass;
    }

}
