import json
import os
import subprocess
import time
import datetime

# required since it executes differently depending on where the main file is called from
try:
    # python3 main.py
    import git
    import gradle
except:
    # python3 versioning/main.py
    import versioning.git
    import versioning.gradle


def findPhase(ver):
    if "-a" in ver: return "alpha"
    if "-b" in ver: return "beta"
    if "-rc" in ver: return "release-candidate"
    return "release"

repoUrl = f"https://github.com/{os.getenv("GITHUB_REPO")}"

username = os.getenv("GITHUB_USERNAME")
email = os.getenv("GITHUB_EMAIL")
version = (os.getenv("GITHUB_REF") or "refs/tags/0.0.0-alpha").replace("refs/tags/", "")
phase = findPhase(version)

f = open("versions.json", "r")
contents = f.read()
f.close()

contents = json.loads(contents)

contents["latest"]["*"] = version
contents["latest"][phase] = version

if not phase in contents["existing-phases"]:
    contents["existing-phases"].append(phase)

contents["versions"][version] = {
    "epoch": int(time.time()),
    "date": datetime.datetime.now().astimezone(datetime.timezone.utc).strftime("%Y/%m/%dT%H:%M:%S"),
    "id": version,
    "phase": phase,
    "maven-jitpack": f"com.github.PuzzlesHQ:puzzle-loader-core:{version}",
    "maven-central": f"dev.puzzleshq:puzzle-loader-core:{version}",
    "dependencies": f"https://github.com/PuzzlesHQ/puzzle-loader-core/releases/download/{version}/dependencies.json"
}

f = open("versions.json", "w")
f.write(json.dumps(contents, indent="\t"))
f.close()

git.init_credentials("github-actions", "github-actions@github.com")
git.checkout("main", True)

gradle.run(task_name="mkDeps")
subprocess.call(args=["gh", "release", "upload", version, "./dependencies.json"])
git.commit(f"add {version} to version manifest", "versions.json")
git.push()