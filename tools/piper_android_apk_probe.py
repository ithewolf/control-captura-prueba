#!/usr/bin/env python3
"""Verify Lab4 native payload only. This is NOT proof of working on-device TTS."""
import json,pathlib,zipfile
apks=list(pathlib.Path("app/build/outputs/apk/debug").glob("*.apk"))
if len(apks)!=1:raise RuntimeError(f"Expected one APK, found {apks}")
apk=apks[0]
with zipfile.ZipFile(apk) as z:
    bad=z.testzip()
    if bad:raise RuntimeError(f"Corrupt APK member: {bad}")
    native=[n for n in z.namelist() if n.endswith(".so") and n.startswith("lib/")]
    sherpa=[n for n in native if ("sherpa" in n.lower() or "onnx" in n.lower())]
    abis=sorted({n.split("/")[1] for n in native})
    if not sherpa:raise RuntimeError("Sherpa native runtime absent from APK")
    if "arm64-v8a" not in abis or "armeabi-v7a" not in abis:
        raise RuntimeError("Missing ARM32/ARM64 support")
    report={"apk_bytes":apk.stat().st_size,"abis":abis,
       "sherpa_native_binaries":sherpa,
       "status":"Piper-compatible native runtime packaged; no Piper model or UI integration",
       "not_validated":["Actual speech playback","Vosk coexistence",
       "memory usage","gameplay FPS","device startup"]}
p=pathlib.Path("build/piper-android-packaging/report.json")
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(json.dumps(report,indent=2),encoding="utf-8")
print(json.dumps(report,indent=2))
