package dev.puzzleshq.puzzleloader.loader.provider.mixin.service;

import dev.puzzleshq.puzzleloader.loader.launch.FlexPiece;
import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class FlexMixinServiceBootstrap implements IMixinServiceBootstrap {

    @Override
    public String getName() {
        return "FlexMixinService";
    }

    @Override
    public String getServiceClassName() {
        return FlexMixinService.class.getName();
    }

    @Override
    public void bootstrap() {
    }
}
