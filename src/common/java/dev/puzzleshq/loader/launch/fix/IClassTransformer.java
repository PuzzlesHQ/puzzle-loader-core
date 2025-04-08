package dev.puzzleshq.loader.launch.fix;

import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

import java.lang.annotation.Annotation;

public interface IClassTransformer extends ILegacyClassTransformer {
    byte[] transform(String name, String fileName, byte[] bytes);

    @Override
    default byte[] transformClassBytes(String s, String s1, byte[] bytes) {
        return transform(s, s1, bytes);
    }

    @Override
    default String getName() {
        return getClass().getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    default boolean isDelegationExcluded() {
        try {
            IClassProvider classProvider = MixinService.getService().getClassProvider();
            Class<? extends Annotation> clResource = (Class<? extends Annotation>)classProvider.findClass("javax.annotation.Resource");
            return getClass().getAnnotation(clResource) != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
