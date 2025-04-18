package dev.puzzleshq.loader.provider.lang;

import dev.puzzleshq.loader.provider.ProviderException;
import dev.puzzleshq.loader.provider.lang.impl.JavaLangProvider;
import dev.puzzleshq.mod.info.ModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public interface ILangProvider {

    Logger LOGGER = LoggerFactory.getLogger("Puzzle | Lang Providers");

    Map<String, ILangProvider> PROVDERS = new HashMap<>();

    ILangProvider JAVA_INSTANCE = new JavaLangProvider();

    static void init() {
        if (!ILangProvider.PROVDERS.containsKey("java")) {
            ILangProvider.PROVDERS.put("java", ILangProvider.JAVA_INSTANCE);
        }
    }

    <T> T create(ModInfo info, String value, Class<T> type) throws ProviderException;

    static ILangProvider getDefault() {
        return JAVA_INSTANCE;
    }


}
