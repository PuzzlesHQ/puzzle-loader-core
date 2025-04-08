package dev.puzzleshq.minecraft.launch;

import org.hjson.JsonObject;

import java.io.*;

public class MinecraftAssetDictionary {

    static JsonObject object;
    static JsonObject assets = new JsonObject();
    static File dir;
    static File gameDir;

    public static void setup(File absoluteFile, String indexStr, File dir) {
        try {
            MinecraftAssetDictionary.gameDir = absoluteFile;
            MinecraftAssetDictionary.dir = dir;
            File index = new File(dir, "indexes/" + indexStr + ".json");
            FileInputStream stream = new FileInputStream(index);

            String s = new String(stream.readAllBytes());
            stream.close();
            object = JsonObject.readJSON(s).asObject();
            assets = object.get("objects").asObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getAllResources() {
        for (String s : assets.names()) getAsset(s);
    }

    public static File getAsset(String s) {
        File assetFile = new File(MinecraftAssetDictionary.gameDir, "resources/" + s);
        if (assetFile.exists()) return assetFile;

        String hash = assets.get(s).asObject().get("hash").asString();
        File hashFile = new File(MinecraftAssetDictionary.dir, "objects/" + hash.substring(0, 2) + "/" + hash);
        assetFile.getParentFile().mkdirs();
        try {
            assetFile.createNewFile();

            InputStream in = new FileInputStream(hashFile);
            OutputStream out = new FileOutputStream(assetFile);
            out.write(in.readAllBytes());
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return assetFile;
    }

}
