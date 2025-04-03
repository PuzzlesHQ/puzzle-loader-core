package com.github.puzzle.loader.util;

public enum EnvType {
    UNKNOWN("UNKNOWN"),
    CLIENT("CLIENT"),
    SERVER("SERVER");

    public final String name;

    EnvType(String sideName) {
        this.name = sideName;
    }
}
