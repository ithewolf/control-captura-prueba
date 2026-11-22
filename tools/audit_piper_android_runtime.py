#!/usr/bin/env python3
"""Audit official Android distribution assets that match Piper's verified v1.12.40 runtime.
No APK is changed and no library is silently shipped.
"""
import json,os,pathlib,urllib.request
VERSION="v1.12.40"
url=f"https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/tags/{VERSION}"
req=urllib.request.Request(url,headers={"Accept":"application/vnd.github+json","User-Agent":"CONTROL-Coach-piper-android-audit"})
with urllib.request.urlopen(req,timeout=45) as r: release=json.load(r)
assets=[]
for a in release["assets"]:
 name=a["name"]
 if "android" in name.lower() or name.lower().endswith(".aar"):
  assets.append({"name":name,"bytes":a["size"],"url":a["browser_download_url"]})
if not assets: raise RuntimeError(f"No official Android distribution for {VERSION}; cannot integrate runtime yet")
report={"sherpa_onnx_version":VERSION,"android_assets":assets,
 "model":"vits-piper-es_MX-ald-medium",
 "model_status":"synthesized successfully on Linux with matching sherpa-onnx 1.12.40",
 "android_status":"Native Android distribution discovered only; NOT yet integrated or tested",
 "constraints":["Android minSdk 29","ARM64 and ARM32 need explicit ABI audit",
                "Actual TTS not yet verified on device","Keep device TTS fallback"]}
root=pathlib.Path("build/piper-android-audit");root.mkdir(parents=True,exist_ok=True)
(root/"runtime-assets.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
print(json.dumps(report,indent=2))
