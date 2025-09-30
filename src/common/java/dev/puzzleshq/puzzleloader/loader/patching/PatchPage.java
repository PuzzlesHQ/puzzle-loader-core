package dev.puzzleshq.puzzleloader.loader.patching;

import java.util.List;

public class PatchPage {

    List<byte[]> patchByteList;
    String endingSHA;

    public PatchPage(List<byte[]> patchByteList, String endingSHA) {
        this.patchByteList = patchByteList;
        this.endingSHA = endingSHA;
    }

    public List<byte[]> getPatchByteList() {
        return patchByteList;
    }

    public String getEndingSHA() {
        return endingSHA;
    }
}
