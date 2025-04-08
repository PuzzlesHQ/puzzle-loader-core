package dev.puzzleshq.loader.mod.info;

import dev.puzzleshq.loader.mod.info.spec.ModJsonV0;
import dev.puzzleshq.loader.mod.info.spec.ModJsonV1;
import dev.puzzleshq.loader.mod.info.spec.ModJsonV2;
import dev.puzzleshq.loader.util.EnvType;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.Collection;
import java.util.Map;

public abstract class ModJson {

    public static int latestRevision = 2;

    public static ModJson fromString(String string) {
        JsonObject obj = (JsonObject) JsonValue.readHjson(string);
        int version = obj.getInt("formatVersion", 0);

        ModJson json;
        switch (version) {
            case 0: json = ModJsonV0.fromString(string); break;
            case 1: json = ModJsonV1.fromString(string); break;
            case 2: json = ModJsonV2.fromString(string); break;
            default: throw new RuntimeException("Invalid ModJson Version $" + version);
        }

        return convert(json, latestRevision);
    }

    public abstract int getRevision();

    public abstract String id();

    public abstract String version();

    public abstract String name();

    public abstract String description();

    public abstract String[] authors();

    public abstract Map<String, Collection<AdapterPathPair>> entrypoints();

    public abstract Map<String, JsonValue> meta();

    public abstract Pair<EnvType, String>[] mixins();

    public abstract Map<String, Pair<String, Boolean>> dependencies();

    public abstract String[] accessTransformers();

    public SideRequire allowedSides() {
        return SideRequire.BOTH_REQUIRED;
    }

    public static ModJson convert(ModJson old, int revision) {
        if (old.getRevision() == revision) return old;

        switch (revision) {
            case 0: return ModJsonV0.transform(old);
            case 1: return ModJsonV1.transform(old);
            case 2: return ModJsonV2.transform(old);
            default: throw new IllegalStateException("Unexpected revision: " + revision);
        }
    }
}
