package dev.puzzleshq.puzzleloader.loader;

public class LoaderConfig {

    public static String PATCH_PAMPHLET_FILE = null;
    public static boolean TRANSFORMERS_ENABLED;
    public static boolean USER_TRANSFORMERS_ENABLED;
    public static boolean DUMP_TRANSFORMED_CLASSES;
    public static boolean ALLOWS_CLASS_OVERRIDES;
    public static String[] COMMAND_LINE_ARGUMENTS;

    public static String MOD_JSON_NAME = "puzzle.mod.json";
    public static String PATCH_JSON_NAME = "puzzle.patch.json";

    public static String CUSTOM_TITLE_FORMAT = "Puzzle Loader: %s";
    public static boolean DO_TITLE_TRANSFORMER = true;
    public static boolean MIXINS_ENABLED = true;

    public static String formatTitle(CharSequence input) {
            return String.format(CUSTOM_TITLE_FORMAT, input);
        }
}