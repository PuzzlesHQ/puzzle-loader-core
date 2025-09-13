package dev.puzzleshq.puzzleloader.loader.provider.lang;

import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.puzzleloader.loader.provider.ProviderException;
import dev.puzzleshq.puzzleloader.loader.provider.lang.impl.JavaLangProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public interface ILangProvider {

    Logger LOGGER = LogManager.getLogger("Puzzle | Lang Providers");

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
