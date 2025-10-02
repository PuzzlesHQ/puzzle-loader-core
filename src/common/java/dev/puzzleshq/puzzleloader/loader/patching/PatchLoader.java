package dev.puzzleshq.puzzleloader.loader.patching;

import dev.puzzleshq.puzzleloader.loader.LoaderConfig;
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
        "client": {
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
            "SHA-256": "d167591a98933317f4397e638b6e622fa09cce52640b06d2cbca176e25742f4d"
        }
    }

    */

    public static PatchPage glanceAtPage(ZipFile zipFile, JsonValue side) throws IOException {
        if (side == null) return null;
        JsonObject object = side.asObject();

        JsonArray array = object.get("patch-order").asArray();
        int patchCount = array.size();

        List<String> headings = new ArrayList<>(patchCount);   // patch-name list
        List<byte[]> paragraphs = new ArrayList<>(patchCount); // patch-contents list

        for (int i = 0; i < patchCount; i++) {
            JsonValue value = array.get(i);
            String patchName = value.asString();
            ZipEntry patchEntry = zipFile.getEntry(patchName);
            InputStream patchStream = zipFile.getInputStream(patchEntry);
            byte[] patchBytes = JavaUtils.readAllBytes(patchStream);
            patchStream.close();
            headings.add(patchName);
            paragraphs.add(patchBytes);
        }

        String endingSHA = object.get("SHA-256").asString();

        return new PatchPage(headings, paragraphs, endingSHA);
    }

    public static PatchPamphlet readPamphlet(File file) throws Exception {
        ZipFile zipFile = new ZipFile(file);

        ZipEntry entry = zipFile.getEntry(LoaderConfig.PATCH_JSON_NAME);
        if (entry == null) throw new RuntimeException("Invalid patch zip provided at \"" + file.getAbsoluteFile() + "\"");
        InputStream jsonInputStream = zipFile.getInputStream(entry);
        byte[] bytes = JavaUtils.readAllBytes(jsonInputStream);
        jsonInputStream.close();
        String content = new String(bytes);
        JsonObject object = JsonValue.readHjson(content).asObject();
        PatchPage clientSide = glanceAtPage(zipFile, object.get("client"));
        PatchPage serverSide = glanceAtPage(zipFile, object.get("server"));
        zipFile.close();

        return new PatchPamphlet(object.get("display-name").asString(), object.get("version").asString(), clientSide, serverSide);
    }

}
