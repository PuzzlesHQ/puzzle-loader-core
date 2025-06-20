package dev.puzzleshq.puzzleloader.loader.util;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads raw assets.
 *
 * @author Mr_Zombii
 * @since 1.0.0
 */
public class RawAssetLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("Puzzle | RawAssetLoader");

    /**
     * Gets the bytes from a {@link InputStream}.
     * @param stream the inputStream to get the bytes from.
     */
    private static byte[] getBytesFromStream(InputStream stream) {
        try {
            byte[] bytes = stream.readAllBytes();
            stream.close();
            return bytes;
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Gets the asset from a zip file.
     * @param file the zip file to get the file from.
     * @param path the path of the file.
     * @return a {@link RawFileHandle}
     */
    public static RawFileHandle getLowLevelZipAsset(ZipFile file, String path) {
        try {
            ZipEntry entry = file.getEntry(path);
            InputStream stream = file.getInputStream(entry);
            String[] strings = entry.getName().split("/");
            return new RawFileHandle(getBytesFromStream(stream), strings[strings.length - 1]);
        } catch (Exception ignore) {
            LOGGER.error("Cannot find resource {} in zip {}.", path, file);
            return null;
        }
    }

    /**
     * Gets the asset from a zip file.
     * @param file the zip file to get the file from.
     * @param location the {@link ResourceLocation} of the file.
     * @return a {@link RawFileHandle}
     */
    public static RawFileHandle getZipAsset(ZipFile file, ResourceLocation location) {
        try {
            ZipEntry entry = file.getEntry(location.toPath());
            InputStream stream = file.getInputStream(entry);
            String[] strings = entry.getName().split("/");
            return new RawFileHandle(getBytesFromStream(stream), strings[strings.length - 1]);
        } catch (Exception ignore) {
            LOGGER.error("Cannot find resource {} in zip {}.", location, file);
            return null;
        }
    }

    /**
     * Gets the asset from the class path.
     * @param path the path of the asset.
     * @return a {@link RawFileHandle}
     */
    public static RawFileHandle getLowLevelClassPathAsset(String path) {
        URL url = LoaderConstants.class.getResource(path);
        if (url == null) {
            try {
                url = PieceClassLoader.class.getClassLoader().getResource(path);
                return new RawFileHandle(getBytesFromStream(url.openStream()), url.getFile());
            } catch (Exception ignore) {
                LOGGER.error("Cannot find resource {} on the classpath.", path);
                return null;
            }
        }
        try {
            return new RawFileHandle(getBytesFromStream(url.openStream()), url.getFile());
        } catch (Exception ignore) {
            LOGGER.error("Cannot find resource {} on the classpath.", path);
            return null;
        }
    }

    /**
     * Gets the asset from the class path.
     * @param location the ResourceLocation of the asset.
     * @return a {@link RawFileHandle}
     */
    public static RawFileHandle getClassPathAsset(ResourceLocation location) {
        URL url = LoaderConstants.class.getResource(location.toPath());
        if (url == null) {
            try {
                url = PieceClassLoader.class.getClassLoader().getResource(location.toPath());
                return new RawFileHandle(getBytesFromStream(url.openStream()), url.getFile());
            } catch (Exception ignore) {
                LOGGER.error("Cannot find resource {} on the classpath.", location);
                return null;
            }
        }
        try {
            return new RawFileHandle(getBytesFromStream(url.openStream()), url.getFile());
        } catch (Exception ignore) {
            LOGGER.error("Cannot find resource {} on the classpath.", location);
            return null;
        }
    }

    /**
     * Gets the asset relative to the directory.
     * @param dir the directory to start at.
     * @param path the asset path.
     * @return a {@link RawFileHandle}
     */
    public static RawFileHandle getLowLevelRelativeAsset(File dir, String path) {
        try {
            URL url = new File(dir, path).toURI().toURL();
            InputStream stream = url.openStream();
            return new RawFileHandle(getBytesFromStream(stream), url.getFile());
        } catch (IOException ignore) {
            LOGGER.error("Cannot find resource relative {} in dir {}", path, dir);
            return null;
        }
    }

    /**
     * Gets the asset relative to the directory.
     * @param dir the directory to start at.
     * @param location the ResourceLocation of the asset.
     * @return a {@link RawFileHandle}
     */
    public static RawFileHandle getRelativeAsset(File dir, ResourceLocation location) {
        try {
            URL url = new File(dir, location.toPath()).toURI().toURL();
            InputStream stream = url.openStream();
            return new RawFileHandle(getBytesFromStream(stream), url.getFile());
        } catch (IOException ignore) {
            LOGGER.error("Cannot find resource relative {} in dir {}", location, dir);
            return null;
        }
    }

    public static class RawFileHandle {

        byte[] bytes;
        String file;

        public RawFileHandle(byte[] bytes, String file) {
            this.bytes = bytes;
            this.file = file;
        }

        /**
         * Gets the file of the RawFileHandle.
         */
        public String getFile() {
            return file;
        }

        /**
         * Gets the bytes of the RawFileHandle.
         */
        public byte[] getBytes() {
            return bytes;
        }

        /**
         * Gets the RawFileHandle as a string.
         */
        public String getString() {
            return new String(getBytes());
        }

        /**
         * Dispose of the RawFileHandle.
         */
        public void dispose() {
            bytes = null;
            file = null;
        }
    }

}
