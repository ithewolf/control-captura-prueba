import io
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile
import zlib

from tools.review_game_frames import FrameReviewError, inspect_png, review_zip


def chunk(tag: bytes, body: bytes) -> bytes:
    return struct.pack(">I", len(body)) + tag + body + struct.pack(
        ">I", zlib.crc32(tag + body) & 0xFFFFFFFF
    )


def png(width=16, height=9) -> bytes:
    # Real minimal RGBA PNG with one filter byte per row.
    line = b"\x00" + bytes([20, 100, 180, 255]) * width
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(line * height))
        + chunk(b"IEND", b"")
    )


def create_zip(path: Path, files: dict[str, bytes]) -> None:
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, contents in files.items():
            archive.writestr(name, contents)


class ReviewTests(unittest.TestCase):
    def test_valid_three_landscape_frames(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "samples.zip"
            create_zip(path, {f"frame_0{i}.png": png() for i in (1, 2, 3)})
            result = review_zip(path)
            self.assertEqual(result["frame_count"], 3)
            self.assertTrue(all(v["ready_for_human_review"] for v in result["frames"]))
            self.assertFalse(result["recognition_performed"])
            self.assertEqual(result["frames"][0]["champion"], "unverified")

    def test_portrait_rejected_from_review_but_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "portrait.zip"
            create_zip(path, {"frame_01.png": png(9, 16)})
            result = review_zip(path)
            self.assertFalse(result["frames"][0]["landscape"])
            self.assertFalse(result["frames"][0]["ready_for_human_review"])

    def test_png_checksum_tampered(self):
        broken = bytearray(png())
        broken[22] ^= 1
        with self.assertRaises(FrameReviewError):
            inspect_png(bytes(broken))

    def test_unknown_file_entry_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "unsafe.zip"
            create_zip(path, {"../secret.txt": b"private", "frame_01.png": png()})
            with self.assertRaisesRegex(FrameReviewError, "unsafe"):
                review_zip(path)

    def test_no_samples_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "empty.zip"
            create_zip(path, {"LEEME.txt": b"empty"})
            with self.assertRaisesRegex(FrameReviewError, "No exported"):
                review_zip(path)

    def test_too_large_sample_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "oversize.zip"
            create_zip(path, {"frame_01.png": b"A" * (6 * 1024 * 1024 + 1)})
            with self.assertRaisesRegex(FrameReviewError, "Invalid frame size"):
                review_zip(path)


if __name__ == "__main__":
    unittest.main()
