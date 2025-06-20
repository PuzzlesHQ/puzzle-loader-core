package dev.puzzleshq.puzzleloader.loader.provider.lang.impl;

import dev.puzzleshq.puzzleloader.loader.provider.ProviderException;
import dev.puzzleshq.puzzleloader.loader.provider.lang.ILangProvider;
import dev.puzzleshq.mod.info.ModInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LangProviderWrapper implements ILangProvider {

    Object clazz;

    public LangProviderWrapper(Object clazz) {
        LOGGER.info("Wrapping Adapter Class \"" + clazz.getClass().getName() + "\"");
        this.clazz = clazz;
    }

    @Override
    public <T> T create(ModInfo info, String value, Class<T> type) throws ProviderException {
        Method create = null;
        try {
            create = clazz.getClass().getDeclaredMethod("create", ModInfo.class, String.class, Class.class);
        } catch (NoSuchMethodException e) {
            throw new ProviderException(e);
        }
        try {
            return (T) create.invoke(clazz, info, value, type);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ProviderException(e);
        }
    }
}
