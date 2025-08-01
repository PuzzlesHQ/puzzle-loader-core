import json
import os
import subprocess
import time
import datetime

def findPhase(ver):
    if "-a" in ver: return "alpha"
    if "-b" in ver: return "beta"
    if "-rc" in ver: return "release-candidate"
    return "release"

ref = os.getenv("GITHUB_REF") or "refs/tags/0.0.0-alpha"
username = os.getenv("GITHUB_USERNAME")
email = os.getenv("GITHUB_EMAIL")
version = ref.replace("refs/tags/", "")
phase = findPhase(version)

f = open("versions.json", "r")
contents = f.read()
f.close()

contents = json.loads(contents)
contents["versions"][version] = {
    "epoch": int(time.time()),
    "date": datetime.datetime.now().astimezone(datetime.timezone.utc).strftime("%Y/%m/%dT%H:%M:%S"),
    "id": version,
    "phase": phase,
    "maven-jitpack": f"com.github.PuzzlesHQ:puzzle-loader-core:{version}",
    "maven-central": f"dev.puzzleshq:puzzle-loader-core:{version}",
    "dependencies": f"https://github.com/PuzzlesHQ/puzzle-loader-core/releases/download/{version}/dependencies.json"
}

contents["latest"] = version
contents["latest-" + phase] = version

if not phase in contents["existing-phases"]:
    contents["existing-phases"].append(phase)

f = open("versions.json", "w")
f.write(json.dumps(contents, indent="\t"))
f.close()

subprocess.call(args=["gradle", "mkDeps"])
subprocess.call(args=["git", "config", "user.name", username])
subprocess.call(args=["git", "config", "user.email", email])
subprocess.call(args=["gh", "release", "upload", version, "./dependencies.json"])
subprocess.call(args=["git", "commit", "-m", f"add {version} to version manifest", "--", "versions.json"])