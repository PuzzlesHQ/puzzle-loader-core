package dev.puzzleshq.loader.mod;

import com.github.zafarkhaja.semver.Version;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import dev.puzzleshq.loader.mod.info.ModInfo;
import dev.puzzleshq.loader.util.ModFinder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public class ModContainer {
    public ModInfo INFO;
    public final EntrypointContainer entrypointContainer;

    public final String NAME;
    public final String ID;
    public final Version VERSION;
    public final ZipFile ARCHIVE;
    public int priority;

    public ModContainer(ModInfo info) {
        this(info, null);
    }

    public ModContainer(@NotNull ModInfo info, ZipFile jar) {
        this.INFO = info;
        this.entrypointContainer = new EntrypointContainer(this, info.Entrypoints);

        NAME = info.DisplayName;
        ID = info.ModID;
        VERSION = info.ModVersion;
        ARCHIVE = jar;
    }

    public <T> void invokeEntrypoint(String key, Class<T> type, Consumer<? super T> invoker) throws Exception {
        entrypointContainer.invokeClasses(key, type, invoker);
    }

    @Override
    public String toString() {
        return "{ ID: \"" + ID + "\", VERSION: \"" + VERSION +"\", PRIORITY: \"" + priority + "\" }";
    }

    public void bumpPriority() {
        Set<Map.Entry<String, Pair<String, Boolean>>> entries = INFO.JSON.dependencies().entrySet();
        for (Map.Entry<String, Pair<String, Boolean>> entry : entries) {
            String modId = entry.getKey();

            if (!ModFinder.getMods().containsKey(modId))
                continue;

            ModContainer dependency = ModFinder.getMod(modId);
            dependency.bumpPriority();
        }
    }
}
