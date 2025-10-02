package dev.puzzleshq.puzzleloader.loader.patching;

import com.google.common.hash.Hashing;
import io.sigpipe.jbsdiff.InvalidHeaderException;
import io.sigpipe.jbsdiff.Patch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class PatchPage {

    private PatchPamphlet parent;
    private final List<String> patchNameList;
    private final List<byte[]> patchByteList;
    private final String checksum;

    public PatchPage(List<String> patchNameList, List<byte[]> patchByteList, String checksum) {
        this.patchNameList = patchNameList;
        this.patchByteList = patchByteList;
        this.checksum = checksum.toLowerCase(Locale.ENGLISH);
    }

    public void setParent(PatchPamphlet parent) {
        if (this.parent != null) throw new RuntimeException("Cannot add more than one parent to a PatchPage twice.");
        this.parent = parent;
    }

    public List<String> getPatchNameList() {
        return patchNameList;
    }

    public List<byte[]> getPatchByteList() {
        return patchByteList;
    }

    public String getChecksum() {
        return checksum;
    }

    public void apply(byte[] bytes, OutputStream out) throws IOException {
        byte[] output = bytes;
        for (int i = 0; i < patchByteList.size(); i++) {
            byte[] patch = patchByteList.get(i);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                Patch.patch(output, patch, stream);
            } catch (InvalidHeaderException | IOException e) {
                throw new RuntimeException("Patch \"" + patchNameList.get(i) + "\" failed.", e);
            }
            output = stream.toByteArray();
            stream.close();
        }
        out.write(output);

        String hashString = Hashing.sha256().hashBytes(output).toString().toLowerCase(Locale.ENGLISH);
        if (checksum.equals(hashString)) return;

        throw new RuntimeException("Patch output does not meet expected hash of \"" + checksum + "\", got \"" + hashString + "\" instead.");
    }

}
