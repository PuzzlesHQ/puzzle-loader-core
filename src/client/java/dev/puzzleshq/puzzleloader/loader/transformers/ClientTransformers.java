package dev.puzzleshq.puzzleloader.loader.transformers;

import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.TransformerInitializer;
import dev.puzzleshq.puzzleloader.loader.provider.classloading.IFlexClassloader;

public class ClientTransformers implements TransformerInitializer {

    public void onTransformerInit(IFlexClassloader classLoader) {
        classLoader.registerTransformer(new ClientASMTransformer());
    }

}
