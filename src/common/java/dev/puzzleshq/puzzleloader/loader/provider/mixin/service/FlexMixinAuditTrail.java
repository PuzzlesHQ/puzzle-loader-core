package dev.puzzleshq.puzzleloader.loader.provider.mixin.service;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.service.IMixinAuditTrail;

public class FlexMixinAuditTrail implements IMixinAuditTrail {

    ILogger logger = new LoggerAdapterConsole("FlexMixinAudit");

    @Override
    public void onApply(String className, String mixinName) {
        logger.info("Applied mixin to " + mixinName + " to class " + className);
    }

    @Override
    public void onPostProcess(String className) {
        logger.info("Postprocessing class " + className);

    }

    @Override
    public void onGenerate(String className, String generatorName) {
        logger.info("Generating class" + className + " with generator " + generatorName);
    }
}
