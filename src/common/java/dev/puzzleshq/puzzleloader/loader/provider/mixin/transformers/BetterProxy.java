package dev.puzzleshq.puzzleloader.loader.provider.mixin.transformers;

import dev.puzzleshq.puzzleloader.loader.launch.fix.IClassTransformer;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

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
    private static MixinTransformer transformer = new MixinTransformer();
    
    /**
     * True if this is the active proxy, newer proxies disable their older
     * siblings
     */
    public boolean isActive = true;
    
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
            return BetterProxy.transformer.transformClassBytes(name, transformedName, basicClass);
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
            return BetterProxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }
        
        return basicClass;
    }

}
