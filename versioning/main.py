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

repoUrl = f"https://github.com/{os.getenv("GITHUB_REPO")}"

username = os.getenv("GITHUB_USERNAME")
email = os.getenv("GITHUB_EMAIL")
version = os.getenv("GITHUB_REF") or "0.0.0-alpha"
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

def initGit():
    subprocess.call(args=["git", "config", "user.name", username])
    subprocess.call(args=["git", "config", "user.email", email])
    subprocess.call(args=["git", "init"])
    subprocess.call(args=["git", "remote", "add", "origin", repoUrl])
    subprocess.call(args=["git", "checkout", "-b", "main"])
    subprocess.call(args=["git", "pull", "origin", "main"])

initGit()

subprocess.call(args=["gradle", "mkDeps"])
subprocess.call(args=["gh", "release", "upload", version, "./dependencies.json"])
subprocess.call(args=["git", "commit", "-m", f"add {version} to version manifest", "--", "versions.json"])
subprocess.call(args=["git", "push", "-u", "origin", "main"])
