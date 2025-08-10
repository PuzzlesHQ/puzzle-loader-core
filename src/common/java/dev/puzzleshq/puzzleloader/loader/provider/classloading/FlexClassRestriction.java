package dev.puzzleshq.puzzleloader.loader.provider.classloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum FlexClassRestriction {

    TRANSFORM_RESTRICTION,
    CLASS_LOADING_RESTRICTION;

    final Map<String, List<String>> restrictedPackages = new HashMap<>();

    FlexClassRestriction() {

    }

    public void add(String cl, String pkg) {
        restrictedPackages.computeIfAbsent(cl, k -> new ArrayList<>());
        restrictedPackages.get(cl).add(pkg);
    }

    public boolean isRestricted(String cl, String pkg) {
        for (String restrictedPackage : restrictedPackages.get(cl)) {
            if (pkg.startsWith(restrictedPackage)) return true;
        }
        return false;
    }

    public static String getRestrictions(String cl, String pkg) {
        StringBuilder out = new StringBuilder();
        for (FlexClassRestriction value : values()) {
            if (value.isRestricted(cl, pkg)) out.append(value.name());
            if (!out.isEmpty()) out.append(',');
        }
        return out.toString();
    }

}
