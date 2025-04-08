package dev.puzzleshq.loader.provider.lang;

import dev.puzzleshq.loader.mod.info.ModInfo;
import dev.puzzleshq.loader.provider.ProviderException;
import dev.puzzleshq.loader.provider.lang.impl.JavaLangProvider;
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
