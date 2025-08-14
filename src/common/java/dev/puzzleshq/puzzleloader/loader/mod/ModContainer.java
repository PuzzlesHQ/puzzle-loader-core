package dev.puzzleshq.puzzleloader.loader.mod;

import com.github.villadora.semver.Version;
import dev.puzzleshq.mod.api.IEntrypointContainer;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.mod.util.ModDependency;

public class ModContainer implements IModContainer {

    private final ModInfo info;
    private final Version version;
    private final String versionStr;
    private int priority = 0;
    private final IEntrypointContainer container;

    public ModContainer(ModInfo info) {
        this.info = info;
        this.version = Version.valueOf(info.getVersion());
        this.versionStr = info.getVersion();
        this.container = new EntrypointContainer(this);
    }

    @Override
    public String getDisplayName() {
        return info.getDisplayName();
    }

    @Override
    public String getID() {
        return info.getId();
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getVersionStr() {
        return versionStr;
    }

    @Override
    public ModInfo getInfo() {
        return info;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void bumpPriority() {
        priority--;

        ModDependency[] dependencies = getInfo().getDependencies();
        for (ModDependency dependency : dependencies) {
            IModContainer dependencyContainer = dependency.getContainer();
            if (dependencyContainer == null)
                continue;

            dependencyContainer.bumpPriority();
        }
    }

    @Override
    public IEntrypointContainer getEntrypointContainer() {
        return container;
    }

    @Override
    public String toString() {
        return "{ DisplayName: " + getDisplayName() + ", ID: " + getID() + ", Version: " + getInfo().getVersion() + ", Loading-Priority : " + getPriority() + " }";
    }
}
