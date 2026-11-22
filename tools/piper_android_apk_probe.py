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
    voice_base="assets/piper/vits-piper-es_MX-ald-medium/"
    voice_assets=[n for n in z.namelist() if n.startswith(voice_base)]
    required=[voice_base+"es_MX-ald-medium.onnx",voice_base+"tokens.txt"]
    if any(x not in voice_assets for x in required):
        raise RuntimeError("Missing Piper model or tokens within actual APK")
    if not any(x.startswith(voice_base+"espeak-ng-data/") for x in voice_assets):
        raise RuntimeError("Missing pronunciation data within actual APK")
    report={"apk_bytes":apk.stat().st_size,"abis":abis,
       "voice_model_bytes":sum(z.getinfo(n).file_size for n in voice_assets),
       "voice_assets_count":len(voice_assets),
       "sherpa_native_binaries":sherpa,
       "status":"Piper-compatible runtime AND voice assets packaged; TTS still NOT wired to Android service",
       "not_validated":["Actual speech playback","Vosk coexistence",
       "memory usage","gameplay FPS","device startup"]}
p=pathlib.Path("build/piper-android-packaging/report.json")
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(json.dumps(report,indent=2),encoding="utf-8")
print(json.dumps(report,indent=2))
