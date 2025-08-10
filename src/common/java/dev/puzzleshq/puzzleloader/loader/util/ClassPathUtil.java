package dev.puzzleshq.puzzleloader.loader.util;

import dev.puzzleshq.puzzleloader.loader.launch.FlexPiece;
import dev.puzzleshq.puzzleloader.loader.launch.bootstrap.BootstrapPiece;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * ClassPathUtil
 *
 * @author Mr_Zombii
 * @since 1.0.0
 */
public class ClassPathUtil {

    /**
     * Gets the JVM class path.
     */
    public static String[] getJVMClasspath() {
        String classPath = (String) System.getProperties().get("java.class.path");
        return classPath.split(File.pathSeparator);
    }

    /**
     * Gets the JVM class path urls.
     */
    public static URL[] getJVMClassPathUrls() {
        String[] strings = getJVMClasspath();
        URL[] urls = new URL[strings.length];
        for (int i = 0; i < strings.length; i++) {
            try {
                urls[i] = new File(strings[i]).getAbsoluteFile().toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return urls;
    }

    /**
     * Gets the Puzzle class path
     */
    public static Collection<URL> getPuzzleClasspath() {
        return BootstrapPiece.generalClassloader.classPath;
    }
}
