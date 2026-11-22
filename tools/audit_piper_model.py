#!/usr/bin/env python3
"""Download and audit Sherpa/Piper voice model as an independent lab artifact."""
import hashlib, json, os, pathlib, tarfile, urllib.request
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-es_AR-daniela-high.tar.bz2"
ROOT=pathlib.Path("build/piper-voice-audit")
ROOT.mkdir(parents=True,exist_ok=True)
archive=ROOT/"model.tar.bz2"
request=urllib.request.Request(URL,headers={"User-Agent":"CONTROL-Coach-voice-audit/1.0"})
with urllib.request.urlopen(request,timeout=150) as source,archive.open("wb") as dest:
    while True:
        chunk=source.read(1024*1024)
        if not chunk: break
        dest.write(chunk)
prefix="vits-piper-es_AR-daniela-high/"
required={prefix+"es_AR-daniela-high.onnx",prefix+"tokens.txt"}
seen=set()
with tarfile.open(archive,"r:bz2") as tf:
    for member in tf.getmembers():
        name=member.name.lstrip("./")
        path=pathlib.PurePosixPath(name)
        if path.is_absolute() or ".." in path.parts or member.issym() or member.islnk():
            raise RuntimeError("Unsafe archive member: "+name)
        if name in required and member.size>0: seen.add(name)
    if seen!=required: raise RuntimeError("Missing model/tokens files: "+str(required-seen))
    if not any(x.name.lstrip("./").startswith(prefix+"espeak-ng-data/") for x in tf):
        raise RuntimeError("Missing espeak-ng-data phonemizer")
meta={"source":URL,"sha256":hashlib.sha256(archive.read_bytes()).hexdigest(),
  "archive_bytes":archive.stat().st_size,"verified_files":sorted(seen),
  "status":"voice-model-files-audited; Android runtime NOT integrated",
  "license":"Per-voice license and attribution require review before shipping."}
(ROOT/"audit.json").write_text(json.dumps(meta,indent=2),encoding="utf-8")
print(json.dumps(meta,indent=2))
