package dev.puzzleshq.puzzleloader.loader.util;

import com.github.villadora.semver.SemVer;
import dev.puzzleshq.mod.ModFormats;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.mod.info.ModInfoBuilder;
import dev.puzzleshq.mod.util.ModDependency;
import dev.puzzleshq.puzzleloader.loader.LoaderConfig;
import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.mod.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hjson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Finders mod jars.
 *
 * @author Mr_Zombii
 * @since 1.0.0
 */
public class ModFinder {

    private static File MOD_FOLDER = null;

    private static final Map<String, Integer> MODS = new HashMap<>();
    private static final List<IModContainer> MODS_ARRAY = new ArrayList<>();

    public static final Logger LOGGER = LogManager.getLogger("Puzzle | ModFinder");

    private static Boolean hasModFinderVerified = false;
    private static Boolean hadModSortingFinished = false;

    /**
     * Sets the mod folder.
     * @param modFolder the folder to set.
     */
    public static void setModFolder(File modFolder) {
        if (modFolder == null)
            throw new RuntimeException(new NullPointerException("ModFolder cannot be null and must exist on your hard drive."));
        if (!modFolder.exists())
            modFolder.mkdirs();
        if (!modFolder.isAbsolute())
            throw new RuntimeException(new FileNotFoundException("ModFolder must be an absolute path."));

        MOD_FOLDER = modFolder;
    }

    /**
     * Gets the mod folder.
     */
    public static File getModFolder() {
        return MOD_FOLDER;
    }

    /**
     * Crawls the mods folder for jars and adds them to classLoader.
     */
    public static void crawlModsFolder() {
        File folder = ModFinder.getModFolder();
        File[] files = folder.listFiles();
        assert files != null;

        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                LOGGER.debug("Found Jar at \"{}\"", file);
                try {
                    Piece.classLoader.addURL(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    LOGGER.error("File {} could not be added to Classpath.", file, e);
                }
            }
        }
    }

    public static final String modOutputFormatter = "\t\u001B[34mname\u001B[37m: %s, \u001B[34mid\u001B[37m: %s, \u001B[34mversion\u001B[37m: %s\u001b[0m";
    public static final String dependencyOutputFormatter = "\t\t > \u001B[35mid\u001B[37m: %s, \u001B[35mconstraint\u001B[37m: %s, \u001B[35moptional\u001B[37m: %s\u001b[0m";
    public static final String missingDependencyOutputFormatter = "\t\t \u001b[9m> \u001B[35mid\u001B[37m: %s, \u001B[35mconstraint\u001B[37m: %s, \u001B[35moptional\u001B[37m: %s\u001b[0m";

    /**
     * Finds the puzzle.mod.json in all the mod jars and adds it.
     */
    public static void findMods() {
        ModFormats.initDefaultFormats();

        if (hadModSortingFinished && hasModFinderVerified) {
            ModFinder.LOGGER.warn("Cannot call ModFinder.findMods() more than once.");
            return;
        }

        Collection<URL> classPath = ClassPathUtil.getPuzzleClasspath();

        addPuzzleCoreBuiltin();

        for (URL url : classPath) {
            if (url.getFile().endsWith(".jar")) {
                try {
                    URLConnection connection = url.openConnection();
                    InputStream inputStream = connection.getInputStream();
                    ZipInputStream stream = new ZipInputStream(inputStream);
                    ZipEntry entry;
                    while ((entry = stream.getNextEntry()) != null) {
                        if (!entry.getName().equals(LoaderConfig.MOD_JSON_NAME)) continue;

                        byte[] bytes = JavaUtils.readAllBytes(stream);

                        System.out.println(url.getFile());
                        addModToArray(ModInfo.readFromString(new String(bytes)), null);
                        break;
                    }
                    stream.close();
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("File \"{}\" may be corrupted or location could not be accessed.", url.getFile(), e);
                }
                continue;
            }
            File file = null;
            try {
                file = new File(URLDecoder.decode(url.getFile(), Charset.defaultCharset().name()));
            } catch (UnsupportedEncodingException ignore) {}

            if (file != null && file.isDirectory()) {
                ModFinder.walk(file);
            }
        }
        verify();

        System.out.println("\nPuzzle Mods:");
        for (int i = 0; i < MODS_ARRAY.size(); i++) {
            IModContainer container = MODS_ARRAY.get(i);
            System.out.printf(modOutputFormatter + "\n", container.getDisplayName(), container.getID(), container.getVersion());
            for (ModDependency dependency : container.getInfo().getDependencies()) {
                IModContainer dependencyContainer = dependency.getContainer();
                if (!(dependencyContainer instanceof IModContainer)) {
                    System.out.printf(missingDependencyOutputFormatter + "\n", dependency.getModID(), dependency.getConstraintStr(), dependency.isOptional());
                } else {
                    System.out.printf(dependencyOutputFormatter + "\n", dependency.getModID(), dependency.getConstraintStr(), dependency.isOptional());
                }
            }
        }
        System.out.println();
    }

    /**
     * Walks the directory non-recursive.
     * @param file the directory to walk.
     */
    private static void walk(File file) {
        Queue<File> fileQueue = new ConcurrentLinkedQueue<>();
        fileQueue.add(file);

        while (!fileQueue.isEmpty()) {
            File f = fileQueue.poll();
            if (!f.exists()) continue;

            if (f.isDirectory()) {
                fileQueue.addAll(Arrays.asList(Objects.requireNonNull(f.listFiles())));
                continue;
            }
            if (f.getName().equals(LoaderConfig.MOD_JSON_NAME)) {
                try {
                InputStream jsonInputStream = new FileInputStream(f);

                byte[] bytes = JavaUtils.readAllBytes(jsonInputStream);
                jsonInputStream.close();

                addModToArray(ModInfo.readFromString(new String(bytes)), null);
                } catch (FileNotFoundException e) {
                    LOGGER.error("Could not find file \"{}\"", f, e);
                } catch (IOException e) {
                    LOGGER.error("Could not read or close file \"{}\"", f, e);
                }
            }
        }
    }

    /**
     * Adds the mod.
     * @param info the mod-json for the mod.
     * @param jar the jar of the mod.
     */
    private static void addModToArray(@Nonnull ModInfo info, @Nullable JarFile jar) {
        if (!info.getLoadableSides().get(Piece.getSide().name)) {
            LOGGER.warn("Found Mod \"{}\" that cannot be launched on the \"{}\", skipping.", info.getId(), Piece.getSide().name);
            return;
        }

        if (MODS.containsKey(info.getId()))
            throw new RuntimeException("Found Duplicate Mod \"{" + info.getId() + "}\", Version: \"" + info.getVersion());

//        LOGGER.info(
//                "Discovered Mod {DisplayName: \"{}\", ID: \"{}\", Version: \"{}\"}",
//                info.getDisplayName(), info.getId(), info.getVersion()
//        );

//        for (ModDependency dependency : info.getDependencies()) {
//            LOGGER.info("\t↪ Dependency: { id: {}, version-constraint: {} }", dependency.getModID(), dependency.getConstraintStr());
//        }

        IModContainer container = new ModContainer(info);
        MODS.put(container.getID(), MODS_ARRAY.size());
        MODS_ARRAY.add(container);
    }

    /**
     * Verifies the mods and their dependency.
     * @see IModContainer
     */
    private static void verify() {
        for (IModContainer container : ModFinder.getModsArray()) {
            ModDependency[] dependencies = container.getInfo().getDependencies();
            for (ModDependency dependency : dependencies) {
                IModContainer dependencyContainer = dependency.getContainer();
                if (dependencyContainer == null) {
                    if (!dependency.isOptional()) throw new RuntimeException(new NullPointerException("Missing dependency \"" + dependency.getModID() + "\" that was required by \"" + container.getID() + "\""));
                    continue;
                }

                boolean doesSatisfy = SemVer.satisfies(dependencyContainer.getVersion(), dependency.getConstraint());
                if (doesSatisfy) continue;
                throw new RuntimeException(new InputMismatchException("Dependency mis-match for mod \"" + container.getID() + "\", it wanted \"" + dependencyContainer.getID() + "\": \"" + dependency.getConstraint() + "\" but got \"" + dependencyContainer.getID() + "\": \"" + dependencyContainer.getVersion() + "\""));
            }
        }
        hasModFinderVerified = true;
        sort();
    }

    /**
     * Sorts the mods.
     * @see IModContainer
     */
    private static void sort() {
        for (IModContainer container : ModFinder.MODS_ARRAY) {
            ModDependency[] dependencies = container.getInfo().getDependencies();
            for (ModDependency dependency : dependencies) {
                IModContainer dependencyContainer = dependency.getContainer();
                if (dependencyContainer == null)
                    continue;

                dependencyContainer.bumpPriority();
            }
        }
        ModFinder.MODS_ARRAY.sort(Comparator.comparingInt(IModContainer::getPriority));
        // Re-Order ModContainer to get the right indices.
        for (IModContainer container : ModFinder.MODS_ARRAY) ModFinder.MODS.put(container.getID(), ModFinder.MODS_ARRAY.indexOf(container));
        ModFinder.hadModSortingFinished = true;
    }

    /**
     * Adds puzzle-Core as a mod.
     */
    private static void addPuzzleCoreBuiltin() {
        if (LoaderConfig.MIXINS_ENABLED) {
            ModInfoBuilder mixinModInfo = new ModInfoBuilder();
            {
                mixinModInfo.setDisplayName("Sponge Mixin Fabric");
                mixinModInfo.setId("sponge-mixin-fabric");
                mixinModInfo.setDescription("Mixin is a trait/mixin framework for Java using ASM.");

                mixinModInfo.addMeta("icon", JsonObject.valueOf("puzzle-loader-core:icons/mixin-icon.png"));
                mixinModInfo.addAuthor(
                        "Mumfrey", "skinnyBat", "LlamaLad7",
                        "Aaron1011", "simon816", "shartte",
                        "zml2008", "bloodmc", "SizableShrimp",
                        "The-PPWD", "LexManos", "jordin",
                        "gabizou", "xIGBClutchIx", "asiekierka",
                        "Runemoro", "caseif", "modmuss50",
                        "JBYoshi", "AbrarSyed", "Kobata",
                        "progwml6"
                );
                mixinModInfo.setVersion(LoaderConstants.FULL_MIXIN_VERSION);
            }
            ModFinder.addModWithContainer(new ModContainer(mixinModInfo.build()));
        }

        Piece.gameProvider.addBuiltinMods();
    }


    /**
     * Adds a mod with a {@link IModContainer}.
     * @param container the {@link IModContainer} to add.
     */
    public static void addModWithContainer(IModContainer container) {
        if (hasModFinderVerified)
            LOGGER.warn("Cannot add mod container \"{}\" after mod dependency verification has taken place.", container.getID());

        MODS.put(container.getID(), MODS_ARRAY.size());
        MODS_ARRAY.add(container);
    }

    /** Checks if a mod was loaded from class-path/mod-folder.
     * @param id the id of the mod.
     * @return a {@link Boolean}
     */
    public static boolean isModLoaded(String id) {
        return ModFinder.MODS.containsKey(id);
    }

    /**
     * Gets the mod of the id.
     * @param id the id of the mod.
     * @return a {@link IModContainer}
     */
    public static IModContainer getMod(String id) {
        Integer dependencyIndex = ModFinder.MODS.get(id);
        if (dependencyIndex == null) {
//            LOGGER.error("Could not find mod \"{}\"", id);
            return null;
        }
        return ModFinder.MODS_ARRAY.get(dependencyIndex);
    }

    /**
     * Gets the mods.
     * @return a {@link Map} of the mods id and index in the MODS_ARRAY.
     * @see ModFinder#getModsArray()
     */
    public static Map<String, Integer> getMods() {
        return MODS;
    }

    /**
     * Gets the mod array.
     * @return a {@link List} of {@link IModContainer}.
     * @see ModFinder#getMods()
     */
    public static List<IModContainer> getModsArray() {
        return MODS_ARRAY;
    }

}
