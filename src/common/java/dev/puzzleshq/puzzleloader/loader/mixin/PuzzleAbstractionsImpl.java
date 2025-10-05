package dev.puzzleshq.puzzleloader.loader.mixin;

import com.github.villadora.semver.Version;
import com.moulberry.mixinconstraints.util.Abstractions;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;

public class PuzzleAbstractionsImpl extends Abstractions {

    @Override
    protected boolean isDevEnvironment() {
        Object property = System.getProperty("puzzle.development");
        if (property == null) return false;

        return "true".equals(property.toString());
    }

    @Override
    protected String getModVersion(String modId) {
        IModContainer container = ModFinder.getMod(modId);
        if (container == null) return null;
        return container.getVersionStr();
    }

    @Override
    protected boolean isVersionInRange(String version, String minVersion, String maxVersion, boolean minInclusive, boolean maxInclusive) {
        Version currentVersion = Version.valueOf(version);
        Version min = minVersion == null ? null : Version.valueOf(minVersion);
        Version max = maxVersion == null ? null : Version.valueOf(maxVersion);

        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("minVersion (" + minVersion + ") is greater than maxVersion (" + maxVersion + ")");
        }

        if (min != null) {
            if (minInclusive) {
                if (currentVersion.compareTo(min) < 0) {
                    return false;
                }
            } else {
                if (currentVersion.compareTo(min) <= 0) {
                    return false;
                }
            }
        }

        if (max != null) {
            if (maxInclusive) {
                return currentVersion.compareTo(max) <= 0;
            } else {
                return currentVersion.compareTo(max) < 0;
            }
        }

        return true;
    }

    @Override
    protected String getPlatformName() {
        return "Puzzle Loader";
    }

}
