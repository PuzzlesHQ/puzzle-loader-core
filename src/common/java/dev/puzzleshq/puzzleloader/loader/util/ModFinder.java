package dev.puzzleshq.puzzleloader.loader.util;

import dev.puzzleshq.puzzleloader.loader.LoaderConstants;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.mod.ModContainer;
import dev.puzzleshq.mod.ModFormats;
import dev.puzzleshq.mod.api.IModContainer;
import dev.puzzleshq.mod.info.ModInfo;
import dev.puzzleshq.mod.info.ModInfoBuilder;
import dev.puzzleshq.mod.util.ModDependency;
import javassist.bytecode.DuplicateMemberException;
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
                    if (connection instanceof JarURLConnection) {
                        JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                        JarFile jar = jarConnection.getJarFile();
                        JarEntry entry = jar.getJarEntry("puzzle.mod.json");
                        if (entry != null) {
                            InputStream jsonInputStream = jar.getInputStream(entry);
                            byte[] bytes = jsonInputStream.readAllBytes();
                            jsonInputStream.close();

                            addModToArray(ModInfo.readFromString(new String(bytes)), jar);
                        }
                        continue;
                    }

                    ZipInputStream stream = new ZipInputStream(connection.getInputStream());
                    ZipEntry entry;
                    while ((entry = stream.getNextEntry()) != null) {
                        if (!entry.getName().equals("puzzle.mod.json")) continue;

                        byte[] bytes = stream.readAllBytes();

                        addModToArray(ModInfo.readFromString(new String(bytes)), null);
                        break;
                    }
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
                fileQueue.addAll(Arrays.asList(f.listFiles()));
                continue;
            }
            if (f.getName().equals("puzzle.mod.json")) {
                try {
                InputStream jsonInputStream = new FileInputStream(f);

                byte[] bytes = jsonInputStream.readAllBytes();
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
        if (!info.getLoadableSides().get(LoaderConstants.SIDE.name)) {
            LOGGER.warn("Found Mod \"{}\" at jar \"{}\" that cannot be launched on the \"{}\", skipping.", info.getId(), jar, LoaderConstants.SIDE.name);
            return;
        }

        String text = jar == null ? "Development" : "";
        if (MODS.containsKey(info.getId()))
            throw new RuntimeException(new DuplicateMemberException("Found Duplicate Mod \"{" + info.getId() + "}\" at jar \"" + jar + "\""));

        LOGGER.info(
                "Discovered {} Mod DisplayName: \"{}\", ID: \"{}\", JarFile: \"{}\"",
                text, info.getDisplayName(), info.getId(), jar
        );

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

                boolean doesSatisfy = dependencyContainer.getVersion().satisfies(dependency.getConstraint());
                if (doesSatisfy) continue;
                throw new RuntimeException(new InputMismatchException("Dependency mis-match for mod \"" + dependencyContainer.getID() + "\", it wanted \"" + dependency.getConstraint() + "\" but got \"" + dependencyContainer.getVersion() + "\""));
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
        ModInfoBuilder puzzleCoreModInfo = new ModInfoBuilder();
        {
            puzzleCoreModInfo.setDisplayName("Puzzle Core");
            puzzleCoreModInfo.setId("puzzle-loader-core");
            puzzleCoreModInfo.setDescription("The core mod-loading mechanics of puzzle-loader");

            puzzleCoreModInfo.addMeta("icon", JsonObject.valueOf("puzzle-loader:icons/PuzzleLoaderIconx160.png"));
            puzzleCoreModInfo.addAuthor("Mr-Zombii", "CrabKing");
            puzzleCoreModInfo.addDependency(new ModDependency(Piece.provider.getId(), Piece.provider.getRawVersion(), false));
            puzzleCoreModInfo.setVersion(LoaderConstants.PUZZLE_CORE_VERSION);

            puzzleCoreModInfo.addEntrypoint("transformers", "dev.puzzleshq.puzzleloader.loader.transformers.CommonTransformers");
            if (LoaderConstants.SIDE.equals(EnvType.CLIENT))
                puzzleCoreModInfo.addEntrypoint("transformers", "dev.puzzleshq.puzzleloader.loader.transformers.ClientTransformers");
        }
        ModFinder.addModWithContainer(new ModContainer(puzzleCoreModInfo.build()));

        Piece.provider.addBuiltinMods();
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
            LOGGER.error("Could not find mod \"{}\"", id);
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
