package dev.puzzleshq.puzzleloader.loader.provider.game;

import dev.puzzleshq.puzzleloader.loader.LoaderConfig;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.patching.PatchLoader;
import dev.puzzleshq.puzzleloader.loader.patching.PatchPage;
import dev.puzzleshq.puzzleloader.loader.patching.PatchPamphlet;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.JavaUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public interface IPatchableGameProvider {

    URL getGameJarLocation();
    default @Nullable URL getPatchLocation() {
        return null;
    }

    static void patchAndReload(IGameProvider provider) throws Exception {
        if (!(provider instanceof IPatchableGameProvider)) return;
        IPatchableGameProvider patchableProvider = (IPatchableGameProvider) provider;
        URL jarURL = patchableProvider.getGameJarLocation();
        String providerClassName = patchableProvider.getClass().getName();
        PieceClassLoader pieceClassLoader = Piece.classLoader;
        Collection<URL> sources = pieceClassLoader.sources.stream().filter(u -> !Objects.equals(u, jarURL)).collect(Collectors.toList());;

        boolean applied = IPatchableGameProvider.patch(patchableProvider, jarURL, sources);
        if (!applied) sources.add(jarURL);

        Piece.classLoader = pieceClassLoader = new PieceClassLoader(new ArrayList<>(pieceClassLoader.sources));
        Thread.currentThread().setContextClassLoader(pieceClassLoader);
        Piece.gameProvider = IGameProvider.loadProviderFromString(providerClassName);
    }

    static boolean patch(IPatchableGameProvider patchableProvider, URL jarURL, Collection<URL> sources) throws Exception {
        if (jarURL == null) return false;

        URL patchURL;
        if (LoaderConfig.PATCH_PAMPHLET_FILE == null || LoaderConfig.PATCH_PAMPHLET_FILE.isEmpty())
            patchURL = patchableProvider.getPatchLocation();
        else
            patchURL = new File(LoaderConfig.PATCH_PAMPHLET_FILE).toURI().toURL();

        if (patchURL == null) return false;

        PatchPamphlet patchPamphlet = PatchLoader.readPamphlet(patchURL);
        if (patchPamphlet.isRipped()) return false;

        PatchPage patchPage = patchPamphlet.getClientPatches();
        if (Piece.getSide().equals(EnvType.SERVER)) patchPage = patchPamphlet.getServerPatches();
        if (patchPage == null) return false;

        File file = new File("./.puzzle/patched/" + patchPage.getChecksum().substring(0, 2) + "/" + patchPage.getChecksum() + ".jar");
        if (!file.exists()) {
            InputStream inputStream = jarURL.openStream();
            byte[] bytes = JavaUtils.readAllBytes(inputStream);
            inputStream.close();

            file.getParentFile().mkdirs();

            FileOutputStream out = new FileOutputStream(file);
            patchPage.apply(bytes, out);
            out.close();
        }
        sources.add(file.toURI().toURL());

        return true;
    }

}
