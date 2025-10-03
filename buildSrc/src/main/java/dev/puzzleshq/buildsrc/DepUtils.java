package dev.puzzleshq.buildsrc;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency;

public class DepUtils {

    public static String getClassifier(Dependency dependency) {
        if (dependency instanceof AbstractExternalModuleDependency dependency1) {
            for (DependencyArtifact artifact : dependency1.getArtifacts()) {
                return artifact.getClassifier();
            }
        }
        return null;
    }

}
