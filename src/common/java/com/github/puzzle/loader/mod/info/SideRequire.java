package com.github.puzzle.loader.mod.info;

import com.github.puzzle.loader.util.EnvType;

public class SideRequire {

    private final boolean client, server;

    public SideRequire(
            boolean client,
            boolean server
    ) {
        this.client = client;
        this.server = server;
    }

    public static final SideRequire CLIENT_ONLY = new SideRequire(true, false);
    public static final SideRequire SERVER_ONLY = new SideRequire(false, true);
    public static final SideRequire BOTH_REQUIRED = new SideRequire(true, true);
    public static final SideRequire SIDE_DOES_NOT_MATTER = new SideRequire(false, false);

    public boolean isClientOnly() {
        return client && !server;
    }

    public boolean isServerOnly() {
        return !client && server;
    }

    public boolean isBothRequired() {
        return client && server;
    }

    public boolean isNuhUhSided() {
        return !client && !server;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SideRequire) {
            SideRequire s = (SideRequire) obj;
            return client == s.client && server == s.server;
        }
        return false;
    }

    public boolean isAllowed(EnvType env) {
        if (isNuhUhSided() || isBothRequired()) return true;

        switch (env) {
            case UNKNOWN: return true;
            case CLIENT: return isClientOnly();
            case SERVER: return isServerOnly();
        }

        return true;
    }
}
