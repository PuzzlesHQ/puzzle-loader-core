package dev.puzzleshq.puzzleloader.loader.provider.mixin.properties;

import org.spongepowered.asm.service.IPropertyKey;

public class FlexPin implements IPropertyKey {

    private final String key;

    public FlexPin(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
