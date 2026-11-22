#!/usr/bin/env python3
"""Stage validated Piper voice assets ONLY for Lab4 experimental APK.
Download occurs at build time (never on user's Android phone).
"""
import hashlib, json, pathlib, tarfile, urllib.request
NAME="vits-piper-es_MX-ald-medium"
URL=f"https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/{NAME}.tar.bz2"
root=pathlib.Path("app/build/generated/piperAssets")
out=root/"piper"/NAME
required={f"{NAME}/es_MX-ald-medium.onnx",f"{NAME}/tokens.txt"}
stamp=out/"stage.json"
if stamp.exists() and (out/"es_MX-ald-medium.onnx").is_file() and (out/"tokens.txt").is_file() and (out/"espeak-ng-data").is_dir():
 print("Piper staging already complete");raise SystemExit(0)
out.mkdir(parents=True,exist_ok=True)
archive=root/"piper-model.tar.bz2"
req=urllib.request.Request(URL,headers={"User-Agent":"CONTROL-Lab4-Android-Piper-model-staging"})
if not archive.exists():
 with urllib.request.urlopen(req,timeout=180) as src,archive.open("wb") as dst:
  while True:
   chunk=src.read(1024*1024)
   if not chunk:break
   dst.write(chunk)
with tarfile.open(archive,"r:bz2") as tf:
 members=tf.getmembers()
 names=set()
 for m in members:
  rel=pathlib.PurePosixPath(m.name.lstrip("./"))
  if rel.is_absolute() or ".." in rel.parts or m.issym() or m.islnk():
   raise RuntimeError("Unsafe archive entry: "+m.name)
  if not str(rel).startswith(NAME+"/"):continue
  if m.isfile() and m.size>0:names.add(str(rel))
 if not required.issubset(names) or not any(n.startswith(NAME+"/espeak-ng-data/") for n in names):
  raise RuntimeError("Missing Piper ONNX, tokens or espeak-ng-data in model package")
 for m in members:
  rel=pathlib.PurePosixPath(m.name.lstrip("./"))
  if not str(rel).startswith(NAME+"/"):continue
  target=(root/"piper"/rel)
  if m.isdir():target.mkdir(parents=True,exist_ok=True);continue
  if not m.isfile():continue
  target.parent.mkdir(parents=True,exist_ok=True)
  with tf.extractfile(m) as src,target.open("wb") as dst:
   while True:
    buf=src.read(1024*1024)
    if not buf:break
    dst.write(buf)
info={"model":NAME,"source":URL,"archive_sha256":hashlib.sha256(archive.read_bytes()).hexdigest(),
"status":"Android APK assets staged; runtime voice synthesis NOT yet wired",
"license":"Review voice licensing and attribution before distribution"}
stamp.write_text(json.dumps(info,indent=2),encoding="utf-8")
print(json.dumps(info,indent=2))
