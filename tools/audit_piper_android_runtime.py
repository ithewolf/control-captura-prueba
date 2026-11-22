#!/usr/bin/env python3
"""Audit official Android distribution assets that match Piper's verified v1.12.40 runtime.
No APK is changed and no library is silently shipped.
"""
import json,os,pathlib,urllib.request,zipfile
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
# Inspect, don't ship: identify the Java AAR's device ABIs and native payload.
aar_asset=next((a for a in assets if a["name"]=="sherpa-onnx-1.12.40.aar"),None)
if aar_asset is None: raise RuntimeError("Official matching non-RKNN AAR not found")
aar_path=pathlib.Path("build/piper-android-audit/sherpa.aar")
aar_path.parent.mkdir(parents=True,exist_ok=True)
download=urllib.request.Request(aar_asset["url"],headers={"User-Agent":"CONTROL-Coach-piper-android-audit"})
with urllib.request.urlopen(download,timeout=120) as source,aar_path.open("wb") as dest:
 while True:
  chunk=source.read(1024*1024)
  if not chunk:break
  dest.write(chunk)
if aar_path.stat().st_size!=aar_asset["bytes"]: raise RuntimeError("AAR byte length mismatch")
with zipfile.ZipFile(aar_path) as z:
 if z.testzip() is not None:raise RuntimeError("Corrupt AAR")
 native=[x for x in z.namelist() if x.endswith(".so") and "/jni/" in "/"+x]
 abis=sorted({x.split("/")[1] for x in native if x.startswith("jni/")})
 java_classes="classes.jar" in z.namelist()
if not java_classes:raise RuntimeError("AAR lacks classes.jar")
if "arm64-v8a" not in abis:raise RuntimeError("AAR lacks arm64-v8a")
# No automatic dependency added to production build.
report={"sherpa_onnx_version":VERSION,"android_assets":assets,
 "aar_audit":{"bytes":aar_path.stat().st_size,"abis":abis,
              "native_count":len(native),"classes_jar":java_classes},

 "model":"vits-piper-es_MX-ald-medium",
 "model_status":"synthesized successfully on Linux with matching sherpa-onnx 1.12.40",
 "android_status":"Native Android distribution discovered only; NOT yet integrated or tested",
 "constraints":["Android minSdk 29","ARM64 and ARM32 need explicit ABI audit",
                "Actual TTS not yet verified on device","Keep device TTS fallback"]}
root=pathlib.Path("build/piper-android-audit");root.mkdir(parents=True,exist_ok=True)
(root/"runtime-assets.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
print(json.dumps(report,indent=2))
