package dev.puzzleshq.puzzleloader.loader.patching;

import dev.puzzleshq.puzzleloader.loader.util.JavaUtils;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PatchLoader {

    /*

    {
        "display-name": "Patchy The Pirate's Patch",
        "version": "69.69.69-alpha",
        "canMerge": false,
        "client: {
            "patch-order": [
                "binary-patch-one.patch",
                "binary-patch-two.patch",
                "binary-patch-three.patch",
                "binary-patch-four.patch",
                "binary-patch-five.patch",
                "binary-patch-six.patch",
                "binary-patch-seven.patch",
                "binary-patch-eight.patch"
            ],
            "SHA-256": "d45f682e4e2c75d1301efafe630282fbad7a53b513b81053790c244087073748"
        },
        "server": {
            "patch-order": [
                "binary-patch-one.patch",
                "binary-patch-two.patch",
                "binary-patch-three.patch",
                "binary-patch-four.patch",
                "binary-patch-five.patch",
                "binary-patch-six.patch",
                "binary-patch-seven.patch",
                "binary-patch-eight.patch"
            ],
            "SHA-256": "d45f682e4e2c75d1301efafe630282fbad7a53b513b81053790c244087073748"
        }
    }

    */

    public static PatchPage glanceAtPage(ZipFile zipFile, JsonValue side) throws IOException {
        if (side == null) return null;
        JsonObject object = side.asObject();

        JsonArray array = object.get("patch-order").asArray();
        List<byte[]> paragraphs = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            JsonValue value = array.get(i);
            String patchName = value.asString();
            ZipEntry patchEntry = zipFile.getEntry(patchName);
            InputStream patchStream = zipFile.getInputStream(patchEntry);
            byte[] patchBytes = JavaUtils.readAllBytes(patchStream);
            patchStream.close();
            paragraphs.add(patchBytes);
        }

        String endingSHA = object.get("SHA-256").asString();

        return new PatchPage(paragraphs, endingSHA);
    }

    public static PatchPamphlet readPamphlet(File file) throws Exception {
        ZipFile zipFile = new ZipFile(file);

        ZipEntry entry = zipFile.getEntry("puzzle.patch.json");
        if (entry == null) throw new RuntimeException("Invalid patch zip provided at \"" + file.getAbsoluteFile() + "\"");
        InputStream jsonInputStream = zipFile.getInputStream(entry);
        byte[] bytes = JavaUtils.readAllBytes(jsonInputStream);
        jsonInputStream.close();
        String content = new String(bytes);
        JsonObject object = JsonValue.readHjson(content).asObject();
        PatchPage clientSide = glanceAtPage(zipFile, object.get("client"));
        PatchPage serverSide = glanceAtPage(zipFile, object.get("client"));
        zipFile.close();

        return new PatchPamphlet(object.get("display-name").asString(), object.get("version").asString(), clientSide, serverSide);
    }

}
