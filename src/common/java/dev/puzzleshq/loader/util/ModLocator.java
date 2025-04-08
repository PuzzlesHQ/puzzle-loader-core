package dev.puzzleshq.loader.util;

import dev.puzzleshq.loader.Constants;
import dev.puzzleshq.loader.launch.Piece;
import dev.puzzleshq.loader.mod.ModContainer;
import dev.puzzleshq.loader.mod.info.ModInfo;
import dev.puzzleshq.loader.mod.info.ModJson;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("UrlHashCode")
public class ModLocator {

    public static final String PARADOX_SERVER_ENTRYPOINT = "com.github.puzzle.paradox.loader.launch.PuzzlePiece";

    public static final String COSMIC_REACH_SERVER_ENTRYPOINT = "finalforeach.cosmicreach.server.ServerLauncher";
    public static final String COSMIC_REACH_CLIENT_ENTRYPOINT = "finalforeach.cosmicreach.lwjgl3.Lwjgl3Launcher";

    public static Logger LOGGER = LogManager.getLogger("Puzzle | ModLocator");
    public static File MOD_FOLDER = new File("pmods");

    public static Map<String, ModContainer> locatedMods = null;

    public static void addMod(ModContainer container) {
        ModLocator.locatedMods.put(container.ID, container);
    }

    @SuppressWarnings("unused")
    public static boolean isModLoaded(String modId) {
        return locatedMods.get(modId) != null;
    }

    public static void setModFolder(File file) {
        MOD_FOLDER = file;
    }

    static HashSet<File> getFilesRecursive(File parent) {
        HashSet<File> hashSet = new HashSet<>();

        if (!parent.isDirectory()) hashSet.add(parent);
        else for (File file : Objects.requireNonNull(parent.listFiles())) {
            if (file.isDirectory()) hashSet.addAll(getFilesRecursive(file));
            else hashSet.add(file);
        }

        return hashSet;
    }

    static HashSet<File> getFiles(File parent) {
        HashSet<File> hashSet = new HashSet<>();

        if (!parent.isDirectory()) hashSet.add(parent);
        hashSet.addAll(Arrays.asList(Objects.requireNonNull(parent.listFiles())));

        return hashSet;
    }

    public static List<ClassPathEntry> getEntriesOnClasspath(Collection<URL> urlz) {
        Collection<URL> urls = getUrlsOnClasspath(urlz);
        List<ClassPathEntry> entries = new ArrayList<>();

        for (URL url : urls) {
            try {
                File file = new File(url.toURI());

                entries.add(new ClassPathEntry(
                        file,
                        ((Supplier<Boolean>) () -> {
                            try {
                                new ZipFile(file);
                                System.gc();
                                return true;
                            } catch (IOException e) {
                                return false;
                            }
                        }).get(),
                        file.isDirectory()
                ));
            } catch (URISyntaxException e) {
            }
        }

        return entries;
    }

    public static @NotNull void forEachEntryOnClasspath(Collection<URL> urlz, Consumer<ClassPathEntry> consumer) {
        Collection<URL> urls = getUrlsOnClasspath(urlz);

        for (URL url : urls) {
            try {
                File file = new File(url.toURI());

                consumer.accept(new ClassPathEntry(
                        file,
                        ((Supplier<Boolean>) () -> {
                            try {
                                new ZipFile(file);
                                System.gc();
                                return true;
                            } catch (IOException e) {
                                return false;
                            }
                        }).get(),
                        file.isDirectory()
                ));
            } catch (URISyntaxException e) {
            }
        }
    }

    public static @NotNull Collection<URL> getUrlsOnClasspath() {
        return getUrlsOnClasspath(new ArrayList<>());
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static @NotNull Collection<URL> getUrlsOnClasspath(Collection<URL> urlz) {
        Set<URL> urls = new HashSet<>(urlz);

        if (ModLocator.class.getClassLoader() instanceof URLClassLoader) {
            URLClassLoader loader = (URLClassLoader) ModLocator.class.getClassLoader();
            Collections.addAll(urls, loader.getURLs());
        } else {
            for (String url : System.getProperty("java.class.path").split(File.pathSeparator)) {
                try {
                    urls.add(new File(url).toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        return urls;
    }

    public static void getMods(EnvType env) {
        getMods(env, new ArrayList<>());
    }

    public static void walkDir(EnvType env, File file) {
        if (file.isDirectory()) {
            if (file.listFiles() != null) {
                Arrays.stream(Objects.requireNonNull(file.listFiles())).forEach((f) -> ModLocator.walkDir(env, f));
            }
        } else if (file.getName().equals("puzzle.mod.json")) {
            try {
                String strInfo = new String(NativeArrayUtil.readNBytes(new FileInputStream(file), Integer.MAX_VALUE));
                ModJson json = ModJson.fromString(strInfo);
                addMod(env, json, null, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void getMods(EnvType env, Collection<URL> classPath) {
        if (ModLocator.locatedMods != null) {
            return;
        }
        ModLocator.locatedMods = new HashMap<>();

        addPuzzleCoreBuiltin();
        Piece.provider.addBuiltinMods();
        Collection<URL> urls = getUrlsOnClasspath(classPath);

        for (URL url : urls) {
            File file = null;
            try {
                file = new File(URLDecoder.decode(url.getFile(), Charset.defaultCharset().name()));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            if (!file.isDirectory()) {
                try {
                    if (file.exists()) {
                        ZipFile jar = new ZipFile(file, ZipFile.OPEN_READ);
                        ZipEntry modJson = jar.getEntry("puzzle.mod.json");
                        if (modJson != null) {
                            ModJson json = ModJson.fromString(new String(NativeArrayUtil.readNBytes(jar.getInputStream(modJson), Integer.MAX_VALUE)));
                            if (!locatedMods.containsKey(json.id()))
                                addMod(env, json, jar, false);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                walkDir(env, file);
            }
        }
    }

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
                    "Mr-Zombii"
            });
            puzzleCoreModInfo.setVersion(Constants.PUZZLE_CORE_VERSION);

            if (Constants.SIDE.equals(EnvType.CLIENT))
                puzzleCoreModInfo.addEntrypoint("transformers", "dev.puzzleshq.loader.transformers.CoreClientTransformers");
        }
        ModLocator.addMod(puzzleCoreModInfo.build().getOrCreateModContainer());
    }

    private static void addMod(EnvType env, ModJson json, ZipFile jar, boolean isDevMod) {
        if (!json.allowedSides().isAllowed(env)) {
            LOGGER.error("Discovered {} Mod \"{}\" with ID \"{}\" on the wrong side of {}, please remove this mod or fix the puzzle.mod.json", json.name(), isDevMod ? "DevMod" : " ", json.id());
            return;
        }

        LOGGER.info("Discovered {} Mod \"{}\" with ID \"{}\"", json.name(), isDevMod ? "DevMod" : " ", json.id());
        if(locatedMods.containsKey(json.id()))
            throw new RuntimeException("mod id \""+json.id()+"\" already used");
        else
            locatedMods.put(json.id(), new ModContainer(ModInfo.fromModJsonInfo(json), jar));
    }

    public static boolean hasDependencyVersion(@NotNull Version current, @NotNull String wanted){

        if(wanted.isEmpty()) {
            throw new RuntimeException("Invalid dependency version identifier");

        }
        if(wanted.equals("*"))
            return true;
        try {
            return current.otherIs(Version.parse(wanted)) == Version.SIZE_COMP.SAME;
        }
        catch (NumberFormatException e){
            try {
                if (wanted.startsWith(">=")) {
                    wanted = wanted.replace(">=", "");

                    Version.SIZE_COMP check = current.otherIs(Version.parse(wanted));

                    return check == Version.SIZE_COMP.SAME || check == Version.SIZE_COMP.LARGER;
                }
                if (wanted.startsWith(">")) {
                    wanted = wanted.replace(">", "");
                    Version.SIZE_COMP check = current.otherIs(Version.parse(wanted));
                    return check == Version.SIZE_COMP.LARGER;
                }
                if (wanted.contains("...")) {
                    String[] wants = wanted.split("\\.\\.\\.");
                    if (wants.length == 2) {
                        Version.SIZE_COMP check1 = current.otherIs(Version.parse(wants[0]));
                        Version.SIZE_COMP check2 = current.otherIs(Version.parse(wants[1]));

                        return (check1 == Version.SIZE_COMP.SAME || check1 == Version.SIZE_COMP.LARGER) && (check2 == Version.SIZE_COMP.SAME || check2 == Version.SIZE_COMP.SMALLER);
                    }
                }
            }
            catch (NumberFormatException e2) {
                LogManager.getLogger("Version Parser").fatal("Version is invalid or format is not supported");
                return false;
            }
            LogManager.getLogger("Version Parser").fatal("Version is invalid or format is not supported");
            return false;
        }

    }

    public static void verifyDependencies() {
        LOGGER.warn("Warning! Only partial semantic versioning support");
        for(ModContainer mod : locatedMods.values()){
            if (mod.INFO.JSON.dependencies() == null) continue;
            if (mod.INFO.JSON.dependencies().isEmpty()) continue;
            LOGGER.info("Mod deps for {}", mod.ID);
            for (Map.Entry<String, Pair<String, Boolean>> entry : mod.INFO.JSON.dependencies().entrySet()) {
                LOGGER.info("\t{}: {}", entry.getKey(), entry.getValue());
                if (entry.getValue().getRight()) {
                    ModContainer modDep = locatedMods.get(entry.getKey());
                    if (modDep == null) {
                        throw new RuntimeException(String.format("can not find mod dependency: %s for mod id: %s", entry.getKey(), mod.ID));
                    } else {
                        if (!hasDependencyVersion(modDep.VERSION, entry.getValue().getLeft())) {
                            throw new RuntimeException(String.format("Mod id: %s, requires: %s version of %s, got: %s", mod.ID, entry.getValue(), modDep.ID, modDep.VERSION));
                        }
                    }
                }
            }
        }
    }

    public static void crawlModsFolder(Collection<URL> urls) {
        if (!MOD_FOLDER.exists()) {
            if (!MOD_FOLDER.mkdir()) LOGGER.warn("{} could not be created, provide access to java", MOD_FOLDER);
            MOD_FOLDER = MOD_FOLDER.getAbsoluteFile();
            return;
        }

        for (File modFile : Objects.requireNonNull(MOD_FOLDER.listFiles())) {
            try {
                LOGGER.info("Found Jar/Zip {}", modFile);
                urls.add(modFile.toURI().toURL());
            } catch (Exception ignore) {}
        }
    }
}
