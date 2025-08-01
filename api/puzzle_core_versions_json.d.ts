export type StringUrl = string;
export type Phase = "alpha" | "beta" | "release-canadate" | "release";
export type PseudoPhase = Phase | "*";
export type VersionId = string;
export type VersionInfo = {
    "epoch": number; // Release date in UnixTime
    "date": string;  // Release date & time with the timezone UTC+00 `YYYY:MM:DDTHH:MM:SS` ex: `2024/7/31T23:12:00` 
    "id": VersionId; // Version ID ex: `0.0.0-alpha` guarenteed to be semver complient, https://semver.org/
    "phase": Phase;  // Type of release ex: "alpha" | "beta" | "release-canadate" | "release"
    "maven-jitpack": string; // Maven Location on the jitpack.io maven
    "maven-central": string; // Maven Location on maven central
    "dependencies": StringUrl; // URL to the dependencies.json
};
export type LoaderCoreVersionsJson = {
    "latest": Record<PsudoPhase, VersionId>; // Map of the latest version according to phase, you can index into * to get the overall latest release.
    "existing-phases": Phase[]; // Phases that have puzzle-loader-core has entered
    "versions": Record<VersionId, VersionInfo>; // Map of versions that can be indexed into according to phase.
};
 
export declare function getVersionDataAsync(): Promise<LoaderCoreVersionsJson | undefined>;
export declare function getVersionDataImmediate(): LoaderCoreVersionsJson | undefined;
