"""Publish only immutable, persistently signed release APKs after all CI gates pass."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

repo = os.environ["GH_REPO"]
base = Path("app/build/outputs/apk/github/release")
element = json.loads((base / "output-metadata.json").read_text())["elements"][0]
code, version = int(element["versionCode"]), element["versionName"]
if code <= 0 or code > 2147483647 or not version or any(ord(c) < 32 for c in version):
    raise SystemExit("Invalid version")
tag = "github-v" + str(code)
name = "MeiSternenreise-" + str(code) + ".apk"
target = Path(name)
shutil.copyfile(base / element["outputFile"], target)
manifest = Path("MeiSternenreise-update.json")
manifest.write_text(json.dumps({"schema": 1, "versionCode": code, "versionName": version,
    "url": "https://github.com/" + repo + "/releases/download/" + tag + "/" + name,
    "sha256": hashlib.sha256(target.read_bytes()).hexdigest(), "size": target.stat().st_size}, indent=2) + "\n")


def exists(release):
    return subprocess.run(["gh", "release", "view", release], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0


def run(*args):
    subprocess.run(["gh", *args], check=True)


if exists(tag):
    print("Version already published. Increase versionCode for a new release; existing APKs remain immutable.")
else:
    run("release", "create", tag, str(target), str(manifest), "--target", os.environ["GITHUB_SHA"],
        "--title", "MeiSternenreise " + version, "--notes-file", "CHANGELOG.md")
    if not exists("updates"):
        run("release", "create", "updates", "--target", os.environ["GITHUB_SHA"], "--prerelease",
            "--title", "MeiSternenreise · Update-Kanal", "--notes", "Update-Metadaten für direkt installierte APKs.")
    run("release", "upload", "updates", str(manifest), "--clobber")
