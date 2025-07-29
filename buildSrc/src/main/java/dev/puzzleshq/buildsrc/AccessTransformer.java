package dev.puzzleshq.buildsrc;

import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

import java.util.ArrayList;
import java.util.List;

public abstract class AccessTransformer implements TransformAction<TransformParameters.None> {

    Attribute<Boolean> manipulated = Attribute.of("manipulated", Boolean.class);

    @InputArtifact
    abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        try {
            var inp = getInputArtifact().get().getAsFile();
            var out = outputs.file(inp.getName().replace(".jar", "-transformed.jar"));

            List<Relocation> rules = new ArrayList<>();
//        rules.add(new Relocation("org.objectweb", "bundled.org.objectweb"))
//        rules.add(new Relocation("org.spongepowered.include", "bundled"))

            JarRelocator relocator = new JarRelocator(inp, out, rules);
            relocator.run();

            GenericTransformer.transform(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
