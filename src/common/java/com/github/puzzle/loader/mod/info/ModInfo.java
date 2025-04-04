package com.github.puzzle.loader.mod.info;

import com.github.puzzle.loader.mod.ModContainer;
import com.github.puzzle.loader.mod.info.spec.ModJsonV2;
import com.github.puzzle.loader.util.EnvType;
import com.github.puzzle.loader.util.Version;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.ImmutablePair;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import org.hjson.JsonValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.jar.JarFile;

public class ModInfo {
    // Info
    public final String DisplayName;
    public final String ModID;
    public final Version ModVersion;
    public final String Description;
    public final ImmutableCollection<String> Authors;
    public final ImmutableMap<String, JsonValue> Metadata;

    // Entrypoints & Mixins
    public final ImmutableMap<String, ImmutableCollection<AdapterPathPair>> Entrypoints;
    public final List<Pair<EnvType, String>> MixinConfigs;

    // Dependencies
    public final ImmutableMap<String, Pair<Version, Boolean>> Dependencies;

    // Access Transformers
    public final String[] AccessTransformers;

    // Extra Info
    public final ModJson JSON;
    private ModContainer Container;
    public final SideRequire allowedSides;

    public ModInfo(@NotNull final ModJson JSON) {
        this.JSON = JSON;

        DisplayName = JSON.name();
        ModID = JSON.id();
        ModVersion = Version.parse(JSON.version());
        Description = JSON.description();

        Authors = ImmutableList.copyOf(JSON.authors());

        if (JSON.meta() != null) {
            ImmutableMap.Builder<String, JsonValue> MetadataBuilder = ImmutableMap.builder();
            for (String key : JSON.meta().keySet()) {
                MetadataBuilder.put(key, JSON.meta().get(key));
            }
            Metadata = MetadataBuilder.build();
        } else Metadata = ImmutableMap.<String, JsonValue>builder().build();

        ImmutableMap.Builder<String, ImmutableCollection<AdapterPathPair>> EntrypointsBuilder = ImmutableMap.builder();
        for (String key : JSON.entrypoints().keySet()) {
            EntrypointsBuilder.put(key, ImmutableList.copyOf(JSON.entrypoints().get(key)));
        }
        Entrypoints = EntrypointsBuilder.build();

        if (JSON.mixins() != null)
            MixinConfigs = Arrays.asList(JSON.mixins());
        else MixinConfigs = new ArrayList<>();

        if (JSON.dependencies() != null) {
            ImmutableMap.Builder<String, Pair<Version, Boolean>> DependenciesBuilder = ImmutableMap.builder();
            for (String key : JSON.dependencies().keySet()) {
                DependenciesBuilder.put(key, new ImmutablePair<>(
                        Version.parse(JSON.dependencies().get(key).getLeft()),
                        JSON.dependencies().get(key).getRight()
                ));
            }
            Dependencies = DependenciesBuilder.build();
        } else Dependencies = ImmutableMap.of();

        if (JSON.accessTransformers() != null) AccessTransformers = JSON.accessTransformers();
        else AccessTransformers = new String[0];

        allowedSides = JSON.allowedSides();
    }

    @Contract("_ -> new")
    public static @NotNull ModInfo fromModJsonInfo(ModJson info) {
        return new ModInfo(info);
    }

    public ModContainer getOrCreateModContainer() {
        if (Container == null) Container = new ModContainer(this);
        return Container;
    }

    public ModContainer getOrCreateModContainer(JarFile file) {
        if (Container != null) Container = new ModContainer(this, file);
        return Container;
    }

    public static class Builder {

        private String id = null;
        private Version version = null;
        private String name = null;
        private String description = null;
        private List<String> authors = new ArrayList<>();
        private Map<String, Collection<AdapterPathPair>> entrypoints = new HashMap<>();
        private Map<String, JsonValue> meta = new HashMap<>();
        private List<Pair<EnvType, String>> mixins = new ArrayList<>();
        private SideRequire require = SideRequire.BOTH_REQUIRED;

        private Map<String, Pair<String, Boolean>> depends = new HashMap<>();
        private final List<String> accessTransformers = new ArrayList<>();

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = Version.parse(version);
            return this;
        }

        public Builder setVersion(Version version) {
            this.version = version;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDesc(String desc) {
            this.description = desc;
            return this;
        }

        public Builder setAuthors(String[] authors) {
            this.authors = Arrays.asList(authors);
            return this;
        }

        public Builder setAuthors(@NotNull Collection<String> authors) {
            this.authors = Arrays.asList(authors.stream().toArray(String[]::new));
            return this;
        }

        public Builder addAuthors(String... names) {
            this.authors.addAll(Arrays.asList(names));
            return this;
        }

        public Builder addAuthor(String name) {
            this.authors.add(name);
            return this;
        }

        public Builder setEntrypoint(String name, Collection<AdapterPathPair> classes) {
            this.entrypoints.put(name, classes);
            return this;
        }

        public Builder addEntrypoint(String name, String adapter, String clazz) {
            if (this.entrypoints.get(name) != null) this.entrypoints.get(name).add(new AdapterPathPair(adapter, clazz));
            else {
                List<AdapterPathPair> classes = new ArrayList<>();
                classes.add(new AdapterPathPair(adapter, clazz));
                this.entrypoints.put(name, classes);
            }
            return this;
        }

        public Builder addEntrypoint(String name, String clazz) {
            if (this.entrypoints.get(name) != null) this.entrypoints.get(name).add(new AdapterPathPair("java", clazz));
            else {
                List<AdapterPathPair> classes = new ArrayList<>();
                classes.add(new AdapterPathPair("java", clazz));
                this.entrypoints.put(name, classes);
            }
            return this;
        }

        public Builder setEntrypoints(Map<String, Collection<AdapterPathPair>> entrypoints) {
            this.entrypoints = entrypoints;
            return this;
        }

        public Builder setMeta(Map<String, JsonValue> meta) {
            this.meta = meta;
            return this;
        }

        public Builder addMeta(String key, JsonValue value) {
            this.meta.put(key, value);
            return this;
        }

        public Builder setSidedMixinConfigs(List<Pair<EnvType, String>> mixinConfigs) {
            this.mixins = mixinConfigs;
            return this;
        }

        public Builder addSidedMixinConfig(EnvType side, String mixinConfigPath) {
            this.mixins.add(new ImmutablePair<>(side, mixinConfigPath));
            return this;
        }

        public Builder addSidedMixinConfigs(EnvType side, String... mixinConfigPaths) {
            for (String mixin : mixinConfigPaths) {
                this.mixins.add(new ImmutablePair<>(side, mixin));
            }
            return this;
        }

        public Builder setDependenciesV2(Map<String, Pair<String, Boolean>> dependencies) {
            this.depends = dependencies;
            return this;
        }

        public Builder addOptionalDependency(String name, String version) {
            this.depends.put(name, new ImmutablePair<>(version, false));
            return this;
        }

        public Builder addDependency(String name, String version) {
            this.depends.put(name, new ImmutablePair<>(version, true));
            return this;
        }

        public Builder addDependency(String name, String version, Boolean isRequired) {
            this.depends.put(name, new ImmutablePair<>(version, isRequired));
            return this;
        }

        public Builder addAccessManipulator(String transformerPath) {
            accessTransformers.add(transformerPath);
            return this;
        }

        protected String makeId() {
            return id == null ?
                    makeName().replaceAll(" ", "-").toLowerCase(Locale.ROOT) :
                    id;
        }

        protected String makeName() {
            return name == null ? "exampleMod" : name;
        }


        public Builder setSideRequirements(SideRequire require) {
            this.require = require;
            return this;
        }

        public ModInfo build() {
            return new ModInfo(new ModJsonV2(
                    makeName(),
                    makeId(),
                    version != null ? version.toString() : "1.0.0",
                    description,
                    authors.toArray(new String[0]),
                    mixins.toArray(new Pair[0]),
                    accessTransformers.toArray(new String[0]),
                    meta,
                    entrypoints,
                    depends,
                    require
            ));
        }

        @Contract(" -> new")
        public static @NotNull ModInfo.Builder New() {
            return new Builder();
        }

    }

}
