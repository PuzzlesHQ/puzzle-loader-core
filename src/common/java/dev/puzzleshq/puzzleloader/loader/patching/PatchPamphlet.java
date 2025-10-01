package dev.puzzleshq.puzzleloader.loader.patching;

import javax.annotation.Nullable;

public class PatchPamphlet {

    PatchPage clientPatches;
    PatchPage serverPatches;

    String displayName;
    String version;

    public PatchPamphlet(String displayName, String version, PatchPage clientPatches, PatchPage serverPatches) {
        this.displayName = displayName;
        this.version = version;

        this.clientPatches = clientPatches;
        this.serverPatches = serverPatches;

        if (this.clientPatches != null)
            this.clientPatches.setParent(this);

        if (this.serverPatches != null)
            this.serverPatches.setParent(this);

        if (clientPatches == null && serverPatches == null)
            System.out.println("Someone ripped a patch pamphlet (Name: \"" + this.displayName + "\", \"Version\": " + this.version + ") :( {insert_sad_trumpet_noise}");
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVersion() {
        return version;
    }

    public @Nullable PatchPage getClientPatches() {
        return clientPatches;
    }

    public @Nullable PatchPage getServerPatches() {
        return serverPatches;
    }

    public boolean isRipped() {
        return clientPatches == null && serverPatches == null;
    }
}
