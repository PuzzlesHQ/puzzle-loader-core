package com.github.puzzle.loader.util;

import java.util.regex.Pattern;

public class RDVersion extends Version {

    public static final Pattern RDValidator = Pattern.compile("^rd-([0-9]{6}){1}$");

    public RDVersion(String version) {
        super(Integer.parseInt(version.split("-")[1]), 0, 0, Type.ALPHA);
    }
}
