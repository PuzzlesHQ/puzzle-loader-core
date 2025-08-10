package dev.puzzleshq.puzzleloader.loader.provider.mixin.properties;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlexQuarkBoard implements IGlobalPropertyService {

    public static FlexQuarkBoard INSTANCE;

    public FlexQuarkBoard() {
        FlexQuarkBoard.INSTANCE = this;
    }

    Map<IPropertyKey, Object> board = new ConcurrentHashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return new FlexPin(name);
    }

    @Override
    public <T> T getProperty(IPropertyKey key) {
        return getProperty(key, null);
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        board.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T) board.getOrDefault(key, defaultValue);
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        return getProperty(key, defaultValue);
    }

}
