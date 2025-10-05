package dev.puzzleshq.puzzleloader.loader.util;

import dev.puzzleshq.puzzleloader.loader.launch.Piece;

import java.util.regex.Pattern;

/**
 * Resource Location
 *
 * @author Mr_Zombii
 * @since 1.0.0
 */
public class ResourceLocation {

    final String id;
    final int hash;
    final String namespace;
    final String path;

    static final Pattern ID_SPLITTER_PATTERN = Pattern.compile(":");

    private ResourceLocation(String namespace, String path) {
        id = namespace + ":" + path;
        hash = id.hashCode();
        this.path = path;
        this.namespace = namespace;
    }

    /**
     * Gets the ResourceLocation of the nameSpace and  name.
     * @param namespace the nameSpace of the ResourceLocation.
     * @param name the name of the ResourceLocation.
     */
    public static ResourceLocation of(String namespace, String name) {
        return new ResourceLocation(namespace, name);
    }

    /**
     * Gets the ResourceLocation of the id.
     * @param id the id of the ResourceLocation.
     */
    public static ResourceLocation of(String id) {
        String[] parts = ID_SPLITTER_PATTERN.split(id);
        if (parts.length < 2)
            return new ResourceLocation(Piece.gameProvider.getDefaultNamespace(), parts[0]);
        if (parts.length > 2)
            throw new RuntimeException("Invalid ResourceLocation Format \"" + id + "\", must be formatted like \"namespace:path\"");

        return new ResourceLocation(parts[0], parts[1]);
    }

    /**
     * Gets the nameSpace of the ResourceLocation.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Gets the path of the ResourceLocation.
     */
    public String getPath() {
        return path;
    }

    /**
     * Converts the namespace and path into a full path starting at assets.
     */
    public String toPath() {
        return "/assets/" + namespace + (path.startsWith("/") ? path : "/" + path);
    }

    /**
     * Gets the string of the ResourceLocation.
     */
    @Override
    public String toString() {
        return id;
    }

    public String toSoftPath() {
        return "assets/" + namespace + (path.startsWith("/") ? path : "/" + path);
    }
}
