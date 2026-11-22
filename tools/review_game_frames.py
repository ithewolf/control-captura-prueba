#!/usr/bin/env python3
"""Offline review of CONTROL's voluntarily exported gameplay-frame ZIP.

Does NOT recognize game state or upload images. Validates PNG container data
and emits a conservative JSON manifest for later *human*-verified labels.
Standard library only; never extracts untrusted ZIP entries to disk.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import zipfile
import zlib

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
FRAME_PATTERN = re.compile(r"frame_0[1-3]\.png\Z")
MAX_IMAGE_BYTES = 6 * 1024 * 1024
MAX_ARCHIVE_UNPACKED = 20 * 1024 * 1024
MAX_DIMENSION = 4096


class FrameReviewError(ValueError):
    pass


def inspect_png(data: bytes) -> tuple[int, int]:
    """Validate chunk boundaries and CRCs; does not pretend to inspect image contents."""
    if not data.startswith(PNG_SIGNATURE):
        raise FrameReviewError("Missing PNG signature")
    offset = len(PNG_SIGNATURE)
    width = height = 0
    found_ihdr = found_idat = found_iend = False
    while offset + 12 <= len(data):
        chunk_len = struct.unpack_from(">I", data, offset)[0]
        tag = data[offset + 4:offset + 8]
        end = offset + 12 + chunk_len
        if chunk_len > MAX_IMAGE_BYTES or end > len(data):
            raise FrameReviewError("Truncated or oversize PNG chunk")
        payload = data[offset + 8:offset + 8 + chunk_len]
        expected = struct.unpack_from(">I", data, offset + 8 + chunk_len)[0]
        observed = zlib.crc32(tag + payload) & 0xFFFFFFFF
        if observed != expected:
            raise FrameReviewError("PNG chunk checksum mismatch")
        if tag == b"IHDR":
            if found_ihdr or offset != 8 or chunk_len != 13:
                raise FrameReviewError("Invalid PNG header")
            width, height = struct.unpack_from(">II", payload, 0)
            if width == 0 or height == 0 or max(width, height) > MAX_DIMENSION:
                raise FrameReviewError("Unexpected PNG dimensions")
            found_ihdr = True
        elif tag == b"IDAT":
            found_idat = True
        elif tag == b"IEND":
            if chunk_len != 0:
                raise FrameReviewError("Invalid IEND")
            found_iend = True
            offset = end
            break
        offset = end
    if not (found_ihdr and found_idat and found_iend and offset == len(data)):
        raise FrameReviewError("Incomplete PNG file")
    return width, height


def review_zip(path: str | Path) -> dict:
    path = Path(path)
    if not path.is_file():
        raise FrameReviewError("ZIP not found")
    results = []
    try:
        with zipfile.ZipFile(path, "r") as archive:
            entries = archive.infolist()
            if sum(info.file_size for info in entries) > MAX_ARCHIVE_UNPACKED:
                raise FrameReviewError("ZIP decompressed size exceeds safe limit")
            if len(entries) > 12:
                raise FrameReviewError("Unexpected number of files in ZIP")
            names = set()
            for info in entries:
                name = info.filename
                if name in names:
                    raise FrameReviewError("Duplicate ZIP filenames")
                names.add(name)
                if info.is_dir() or name not in {
                    "LEEME.txt", "fechas.txt", "frame_01.png", "frame_02.png", "frame_03.png"
                }:
                    raise FrameReviewError("Unknown or unsafe ZIP entry: " + name)
                if not FRAME_PATTERN.fullmatch(name):
                    continue
                if info.file_size == 0 or info.file_size > MAX_IMAGE_BYTES:
                    raise FrameReviewError("Invalid frame size: " + name)
                pixels = archive.read(info)
                width, height = inspect_png(pixels)
                landscape = width > height
                results.append({
                    "file": name,
                    "sha256": hashlib.sha256(pixels).hexdigest(),
                    "width": width,
                    "height": height,
                    "landscape": landscape,
                    "ready_for_human_review": landscape,
                    "screen_type": "unverified",
                    "champion": "unverified",
                    "match_event": "unverified",
                })
    except (zipfile.BadZipFile, RuntimeError, OSError) as error:
        raise FrameReviewError(f"Cannot read ZIP: {error}") from error
    if not results:
        raise FrameReviewError("No exported gameplay frame_0N.png files")
    results.sort(key=lambda row: row["file"])
    return {
        "schema": "control-vision-review-v1",
        "source": "voluntary_local_export",
        "recognition_performed": False,
        "uploaded_automatically": False,
        "frame_count": len(results),
        "frames": results,
        "note": (
            "PNG format/orientation verified only. Wild Rift, menu type, "
            "champions and gameplay events require actual visual review. "
            "Inspect private player names before sharing any frames."
        ),
    }


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Offline checks for a manually exported CONTROL screenshot ZIP."
    )
    parser.add_argument("zipfile", help="control-frames-wildrift.zip")
    parser.add_argument("--out", help="Optional path to write JSON review manifest")
    args = parser.parse_args()
    report = review_zip(args.zipfile)
    output = json.dumps(report, indent=2, ensure_ascii=False)
    if args.out:
        Path(args.out).write_text(output + "\n", encoding="utf-8")
    print(output)


if __name__ == "__main__":
    main()
