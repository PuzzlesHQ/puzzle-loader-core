package com.github.puzzle.loader.util;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.regex.Pattern;

public class Version implements Comparator<Version>, Comparable<Version> {

    private final int major, minor, patch;
    private final Type type;

    public Version(int major, int minor, int patch, Type t) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.type = t;
    }

    @Override
    public int compare(Version o1, Version o2) {
        return o1.compareTo(o2);
    }

    @Override
    public int compareTo(@NotNull Version o) {
        if (equals(o)) return 0;

        if (o.type.ordinal() > type.ordinal()) return -1;
        if (o.type.ordinal() < type.ordinal()) return 1;

        if (o.major != major) return Integer.compare(major, o.major);
        if (o.minor != minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Version) {
            Version v = (Version) obj;
            return v.major == major && v.minor == minor && v.patch == patch && v.type == type;
        }
        return false;
    }

    public enum Type {

        NONE("0123456789"),
        PRE_ALPHA("p"),
        ALPHA("a"),
        BETA("b"),
        GAMMA("g"),
        RELEASE_CANDIDATE("rc"),
        RELEASE("r");

        final String matcherString;

        Type(String matcherString) {
            this.matcherString = matcherString;
        }

        public static Type find(String typeString) {
            for (Type t : Type.values()) {
                if (t.matcherString.equals(typeString.toLowerCase())) {
                    return t;
                }
            }
            return Type.RELEASE;
        }

        @Override
        public String toString() {
            return matcherString;
        }
    }

    static final Pattern dotSplit = Pattern.compile("\\.");
    static final Pattern dashSplit = Pattern.compile("-");

    static final Pattern validator = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+){1}(-[PRGprgA-Ba-b]|-rc|-RC|)$");

    public static Version parse(String version) {
        if (!validator.matcher(version).matches())
            throw new IllegalArgumentException("Invalid version \"" + version + "\", Version must fit within \"number.number\" or \"number.number.number\" with an optional -a, -b, -r, -p ex:0.4.0-rc");

        String[] parts = dotSplit.split(version);
        String endPart = parts[parts.length - 1];

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = 0;

        Type t = Type.NONE;

        if (endPart.contains("-")) {
            String typeString = dashSplit.split(endPart, 1)[0];

            t = Type.find(typeString);

            if (parts.length > 2)
                patch = Integer.parseInt(endPart.replaceAll("-" + typeString, ""));
        } else {
            if (parts.length > 2)
                patch = Integer.parseInt(endPart);
        }

        return new Version(
                major, minor, patch, t
        );
    }

    public enum SIZE_COMP {
        SAME,
        LARGER,
        SMALLER
    }

    public SIZE_COMP otherIs(@NotNull Version version) {
        if (major == version.major && minor == version.minor && patch == version.patch && version.type.ordinal() == type.ordinal()) return SIZE_COMP.SAME;
        if (type.ordinal() > version.type.ordinal()) return SIZE_COMP.LARGER;
        if (major > version.major && type.ordinal() == version.type.ordinal()) return SIZE_COMP.LARGER;
        if (minor > version.minor && major == version.major && type.ordinal() == version.type.ordinal()) return SIZE_COMP.LARGER;
        if (patch > version.patch && major == version.major && minor == version.minor && type.ordinal() == version.type.ordinal()) return SIZE_COMP.LARGER;
        return SIZE_COMP.SMALLER;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (type == Type.NONE ? "" : "-" + type);
    }
}
