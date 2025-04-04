package com.github.puzzle.loader.provider.lang;

import com.github.puzzle.loader.mod.info.ModInfo;
import com.github.puzzle.loader.provider.ProviderException;
import com.github.puzzle.loader.provider.lang.impl.JavaLangProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public interface ILangProvider {

    Logger LOGGER = LogManager.getLogger("Puzzle | Lang Providers");
    Map<String, ILangProvider> PROVDERS = new HashMap<>();

    ILangProvider JAVA_INSTANCE = new JavaLangProvider();

    <T> T create(ModInfo info, String value, Class<T> type) throws ProviderException;

    static ILangProvider getDefault() {
        return JAVA_INSTANCE;
    }


}
