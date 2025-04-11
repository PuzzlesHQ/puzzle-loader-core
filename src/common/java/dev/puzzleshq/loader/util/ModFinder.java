package dev.puzzleshq.loader.util;

import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import dev.puzzleshq.loader.Constants;
import dev.puzzleshq.loader.launch.Piece;
import dev.puzzleshq.loader.mod.ModContainer;
import dev.puzzleshq.loader.mod.info.ModInfo;
import dev.puzzleshq.loader.mod.info.ModJson;
import javassist.bytecode.DuplicateMemberException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

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
 * @since 4.0.0
 */
public class ModFinder {

    private static File MOD_FOLDER = null;

    private static final Map<String, Integer> MODS = new HashMap<>();
    private static final List<ModContainer> MODS_ARRAY = new ArrayList<>();

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

                            String rawJson = new String(bytes);
                            ModJson modJson = ModJson.fromString(rawJson);

                            addModToArray(modJson, jar);
                        }
                        continue;
                    }

                    ZipInputStream stream = new ZipInputStream(connection.getInputStream());
                    ZipEntry entry;
                    while ((entry = stream.getNextEntry()) != null) {
                        if (!entry.getName().equals("puzzle.mod.json")) continue;

                        byte[] bytes = stream.readAllBytes();
                        String rawJson = new String(bytes);
                        ModJson modJson = ModJson.fromString(rawJson);

                        addModToArray(modJson, null);
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

                String rawJson = new String(bytes);
                ModJson modJson = ModJson.fromString(rawJson);

                addModToArray(modJson, null);
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
     * @param modJson the mod-json for the mod.
     * @param jar the jar of the mod.
     */
    private static void addModToArray(@Nonnull ModJson modJson, @Nullable JarFile jar) {
        if (modJson.allowedSides().isAllowed(Constants.SIDE)) {
            LOGGER.warn("Found Mod \"{}\" at jar \"{}\" that cannot be launched on the \"{}\", skipping.", modJson.id(), jar, Constants.SIDE.name);
            return;
        }

        String text = jar == null ? "Development" : "";
        if (MODS.containsKey(modJson.id()))
            throw new RuntimeException(new DuplicateMemberException("Found Duplicate Mod \"{" + modJson.id() + "}\" at jar \"" + jar + "\""));

        LOGGER.info(
                "Discovered{} Mod DisplayName: \"{}\", ID: \"{}\", JarFile: \"{}\"",
                text, modJson.name(), modJson.id(), jar
        );

        ModContainer container = ModInfo.fromModJsonInfo(modJson).getOrCreateModContainer(jar);
        MODS.put(container.ID, MODS_ARRAY.size());
        MODS_ARRAY.add(container);
    }

    /**
     * Verifies the mods and their dependency.
     * @see ModContainer
     */
    private static void verify() {
        for (ModContainer container : ModFinder.getModsArray()) {
            Set<Map.Entry<String, Pair<String, Boolean>>> entries = container.INFO.JSON.dependencies().entrySet();
            for (Map.Entry<String, Pair<String, Boolean>> entry : entries) {
                Pair<String, Boolean> pair = entry.getValue();
                String modId = entry.getKey();

                if (!MODS.containsKey(modId)) {
                    if (pair.getRight()) throw new RuntimeException(new NullPointerException("Missing dependency \"" + modId + "\" that was required by \"" + container.ID + "\""));
                    continue;
                }

                ModContainer dependency = ModFinder.getMod(modId);

                boolean doesSatisfy = dependency.VERSION.satisfies(pair.getLeft());
                if (doesSatisfy) continue;
                throw new RuntimeException(new InputMismatchException("Dependency mis-match for mod \"" + container.ID + "\", it wanted \"" + pair.getLeft() + "\" but got \"" + dependency.VERSION + "\""));
            }
        }
        hasModFinderVerified = true;
        sort();
    }

    /**
     * Sorts the mods.
     * @see ModContainer
     */
    private static void sort() {
        for (ModContainer container : ModFinder.MODS_ARRAY) {
            Set<Map.Entry<String, Pair<String, Boolean>>> entries = container.INFO.JSON.dependencies().entrySet();
            for (Map.Entry<String, Pair<String, Boolean>> entry : entries) {
                String modId = entry.getKey();

                if (!MODS.containsKey(modId))
                    continue;

                ModContainer dependency = ModFinder.getMod(modId);
                dependency.bumpPriority();
            }
        }
        ModFinder.MODS_ARRAY.sort(Comparator.comparingInt(a -> a.priority));
        // Re-Order ModContainer to get the right indices.
        for (ModContainer container : ModFinder.MODS_ARRAY) ModFinder.MODS.put(container.ID, ModFinder.MODS_ARRAY.indexOf(container));
        ModFinder.hadModSortingFinished = true;
    }

    /**
     * Adds puzzle-Core as a mod.
     */
    private static void addPuzzleCoreBuiltin() {
        ModInfo.Builder puzzleCoreModInfo = ModInfo.Builder.New();
        {
            puzzleCoreModInfo.setName("Puzzle Core");
            puzzleCoreModInfo.setId("puzzle-loader-core");
            puzzleCoreModInfo.setDesc("The core mod-loading mechanics of puzzle-loader");

            HashMap<String, JsonValue> meta = new HashMap<>();
            meta.put("icon", JsonObject.valueOf("puzzle-loader:icons/PuzzleLoaderIconx160.png"));
            puzzleCoreModInfo.setMeta(meta);
            puzzleCoreModInfo.setAuthors(new String[]{
                    "Mr-Zombii", "CrabKing"
            });
            puzzleCoreModInfo.addDependency(Piece.provider.getId(), Piece.provider.getRawVersion());
            puzzleCoreModInfo.setVersion(Constants.PUZZLE_CORE_VERSION);

            if (Constants.SIDE.equals(EnvType.CLIENT))
                puzzleCoreModInfo.addEntrypoint("transformers", "dev.puzzleshq.loader.transformers.CoreClientTransformers");
        }
        ModFinder.addModWithContainer(puzzleCoreModInfo.build().getOrCreateModContainer());
        Piece.provider.addBuiltinMods();
    }


    /**
     * Adds a mod with a {@link ModContainer}.
     * @param container the {@link ModContainer} to add.
     */
    public static void addModWithContainer(ModContainer container) {
        if (hasModFinderVerified)
            LOGGER.warn("Cannot add mod container \"{}\" after mod dependency verification has taken place.", container.ID);

        MODS.put(container.ID, MODS_ARRAY.size());
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
     * @return a {@link ModContainer}
     */
    public static ModContainer getMod(String id) {
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
     * @return a {@link List} of {@link ModContainer}.
     * @see ModFinder#getMods()
     */
    public static List<ModContainer> getModsArray() {
        return MODS_ARRAY;
    }

}
