package dev.puzzleshq.puzzleloader.loader.provider.classloading;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FlexGlobalCLSettings {

    public static final AtomicBoolean DUMP_TRANSFORMED_CLASSES = new AtomicBoolean(false);
    public static final AtomicReference<File> TRANSFORM_DUMP_DIRECTORY = new AtomicReference<>(new File(".transform-class-dump"));

    public static final AtomicBoolean ALLOW_CLASS_OVERRIDING = new AtomicBoolean(false);
    public static final AtomicReference<File> CLASS_OVERRIDES_DIRECTORY = new AtomicReference<>(new File(".class-overrides"));

    public static void loadRegistrationRestrictions(IFlexClassloader classloader) {
        if (classloader.getName().equals("General-FlexClassLoader")) {
            classloader.registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "org.spongepowered.");
            classloader.registerRestriction(FlexClassRestriction.CLASS_LOADING_RESTRICTION, "dev.puzzleshq.puzzleloader.loader.");
        }
    }

    public static URL getOverride(String classFile) {
        File input = new File(CLASS_OVERRIDES_DIRECTORY.get(), classFile);
        try {
            return input.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void dumpClass(String file, byte[] bytes) {
        try {
            File output = new File(TRANSFORM_DUMP_DIRECTORY.get(), file);

            if (!output.getParentFile().exists()) output.getParentFile().mkdirs();
            if (!output.exists()) output.createNewFile();

            FileOutputStream stream = new FileOutputStream(output);
            stream.write(bytes);
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
