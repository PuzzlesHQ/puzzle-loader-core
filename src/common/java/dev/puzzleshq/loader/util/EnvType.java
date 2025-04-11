package dev.puzzleshq.loader.util;

/**
 * The network side
 *
 * @author Mr_Zombii
 * @since 1.0.0
 */
public enum EnvType {
    UNKNOWN("UNKNOWN"),
    CLIENT("CLIENT"),
    SERVER("SERVER");

    public final String name;

    EnvType(String sideName) {
        this.name = sideName;
    }
}
