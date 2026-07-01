#!/usr/bin/env python3
"""Gera publish/ a partir dos modulos em sources/*, lendo metadados de build.gradle.kts.

Cada pasta em sources/<pkg>/ vira uma entrada no indice do repositorio; nao ha
mais um repo-entry.json manual para manter atualizado.
"""
import hashlib
import html
import json
import os
import re
import shutil
from pathlib import Path

REPO_OWNER = "gabrieldesignhd-rgb"
REPO_NAME = "ONGRID-EXTENSOES"
REPO_DISPLAY_NAME = "ONGRID Extensões"
REPO_WEBSITE = f"https://github.com/{REPO_OWNER}/{REPO_NAME}"


def parse_gradle(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")

    def field(pattern: str, default=None):
        match = re.search(pattern, text)
        return match.group(1) if match else default

    return {
        "name": field(r'name\s*=\s*"([^"]+)"'),
        "version_code": int(field(r"versionCode\s*=\s*(\d+)")),
        "content_warning": field(r"contentWarning\s*=\s*ContentWarning\.(\w+)", "SAFE"),
        "lib_version": field(r'libVersion\s*=\s*"([^"]+)"'),
        "lang": field(r'lang\s*=\s*"([^"]+)"'),
        "base_url": field(r'baseUrl\s*=\s*"([^"]+)"'),
    }


def source_id(name: str, lang: str, version_id: int = 1) -> str:
    key = f"{name.lower()}/{lang}/{version_id}"
    digest = hashlib.md5(key.encode()).digest()
    value = 0
    for i in range(8):
        value |= (digest[i] & 0xFF) << (8 * (7 - i))
    return str(value & 0x7FFFFFFFFFFFFFFF)


def main() -> None:
    fingerprint = os.environ["SIGNING_KEY_FINGERPRINT"]
    publish = Path("publish")
    (publish / "apk").mkdir(parents=True, exist_ok=True)
    (publish / "icon").mkdir(parents=True, exist_ok=True)

    entries = []
    for module_dir in sorted(Path("sources").iterdir()):
        if not module_dir.is_dir():
            continue
        pkg = module_dir.name
        meta = parse_gradle(module_dir / "build.gradle.kts")
        version_name = f"{meta['lib_version']}.{meta['version_code']}"
        apk_name = f"tachiyomi-pt.{pkg}-v{version_name}.apk"

        built_apks = list(Path(f"builder/src/pt/{pkg}/build/outputs/apk").rglob("*.apk"))
        if not built_apks:
            raise SystemExit(f"APK nao encontrado para o modulo '{pkg}'")
        shutil.copy(built_apks[0], publish / "apk" / apk_name)

        icon_src = module_dir / "res" / "mipmap-xxxhdpi" / "ic_launcher.png"
        shutil.copy(icon_src, publish / "icon" / f"eu.kanade.tachiyomi.extension.pt.{pkg}.png")

        entries.append(
            {
                "name": f"Tachiyomi: {meta['name']}",
                "pkg": f"eu.kanade.tachiyomi.extension.pt.{pkg}",
                "apk": apk_name,
                "lang": meta["lang"],
                "code": meta["version_code"],
                "version": version_name,
                "nsfw": 1 if meta["content_warning"] == "NSFW" else 0,
                "sources": [
                    {
                        "name": meta["name"],
                        "lang": meta["lang"],
                        "id": source_id(meta["name"], meta["lang"]),
                        "baseUrl": meta["base_url"],
                    }
                ],
            }
        )

    (publish / "index.min.json").write_text(
        json.dumps(entries, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )
    (publish / "index.json").write_text(
        json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (publish / "repo.json").write_text(
        json.dumps(
            {
                "meta": {
                    "name": REPO_DISPLAY_NAME,
                    "website": REPO_WEBSITE,
                    "signingKeyFingerprint": fingerprint,
                },
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    links = "".join(
        f"<p><a href='https://raw.githubusercontent.com/{REPO_OWNER}/{REPO_NAME}/repo/apk/{e['apk']}'>"
        f"{html.escape(e['name'])}</a></p>"
        for e in entries
    )
    (publish / "index.html").write_text(
        "<!doctype html><html><head><meta charset='utf-8'>"
        f"<title>{REPO_DISPLAY_NAME}</title></head><body>"
        f"<h1>{REPO_DISPLAY_NAME}</h1>{links}</body></html>",
        encoding="utf-8",
    )
    (publish / "README.md").write_text(
        f"# {REPO_DISPLAY_NAME}\n\n"
        "URL para adicionar no Mihon:\n\n"
        f"`https://raw.githubusercontent.com/{REPO_OWNER}/{REPO_NAME}/repo/index.min.json`\n",
        encoding="utf-8",
    )

    print("Publicadas " + str(len(entries)) + " fonte(s): " + ", ".join(e["name"] for e in entries))


if __name__ == "__main__":
    main()
