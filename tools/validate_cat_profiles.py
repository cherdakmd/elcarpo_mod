#!/usr/bin/env python3
"""Validate subscriber cat profiles, their variant JSON, and texture dimensions."""

from __future__ import annotations

import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src" / "main" / "resources"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def read_png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as image:
        header = image.read(24)
    if len(header) < 24 or header[:8] != PNG_SIGNATURE or header[12:16] != b"IHDR":
        raise ValueError("not a valid PNG with an IHDR header")
    return struct.unpack(">II", header[16:24])


def load_json(path: Path, errors: list[str]) -> dict | None:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        errors.append(f"{path.relative_to(ROOT)}: invalid JSON ({error})")
        return None
    if not isinstance(value, dict):
        errors.append(f"{path.relative_to(ROOT)}: expected a JSON object")
        return None
    return value


def validate() -> list[str]:
    errors: list[str] = []
    profile_files = sorted(RESOURCES.glob("data/*/cat_profile/**/*.json"))

    for profile_file in profile_files:
        relative = profile_file.relative_to(RESOURCES / "data")
        namespace = relative.parts[0]
        profile_path = Path(*relative.parts[2:]).with_suffix("")
        profile_id = f"{namespace}:{profile_path.as_posix()}"
        profile = load_json(profile_file, errors)
        if profile is None:
            continue

        name = profile.get("name")
        if not isinstance(name, str) or not name.strip():
            errors.append(f"{profile_file.relative_to(ROOT)}: 'name' must be a non-empty string")

        for field in ("agility", "strength", "defense"):
            rating = profile.get(field)
            if isinstance(rating, bool) or not isinstance(rating, int) or not 0 <= rating <= 100:
                errors.append(f"{profile_file.relative_to(ROOT)}: '{field}' must be an integer from 0 to 100")

        variant_file = RESOURCES / "data" / namespace / "cat_variant" / profile_path.with_suffix(".json")
        if not variant_file.is_file():
            errors.append(f"{profile_id}: missing matching cat_variant JSON")
            continue
        variant = load_json(variant_file, errors)
        if variant is None:
            continue

        for asset_field, expected_size in (("asset_id", (64, 32)), ("baby_asset_id", (32, 32))):
            asset_id = variant.get(asset_field)
            if not isinstance(asset_id, str) or ":" not in asset_id:
                errors.append(f"{variant_file.relative_to(ROOT)}: '{asset_field}' must be a namespaced texture ID")
                continue

            texture_namespace, texture_name = asset_id.split(":", 1)
            if not texture_namespace or not texture_name or ".." in Path(texture_name).parts:
                errors.append(f"{variant_file.relative_to(ROOT)}: invalid texture ID '{asset_id}'")
                continue

            texture = RESOURCES / "assets" / texture_namespace / "textures" / f"{texture_name}.png"
            try:
                actual_size = read_png_size(texture)
            except (OSError, ValueError) as error:
                errors.append(f"{profile_id}: cannot read texture {texture.relative_to(ROOT)} ({error})")
                continue
            if actual_size != expected_size:
                errors.append(
                    f"{profile_id}: {asset_field} texture must be {expected_size[0]}x{expected_size[1]}, "
                    f"got {actual_size[0]}x{actual_size[1]}"
                )

    return errors


def main() -> int:
    errors = validate()
    if errors:
        print("Cat profile validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    count = len(list(RESOURCES.glob("data/*/cat_profile/**/*.json")))
    print(f"Cat profile validation passed ({count} profile(s)).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
