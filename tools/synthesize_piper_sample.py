#!/usr/bin/env python3
"""Synthesize a real Piper male voice sample in isolated Linux CI.
Not an Android integration; playback and performance on Android remain untested.
"""
import json, pathlib, tarfile
import numpy as np
import sherpa_onnx
import soundfile as sf

root=pathlib.Path("build/piper-voice-audit")
archive=root/"model.tar.bz2"
if not archive.exists(): raise SystemExit("Run audit_piper_model.py first")
prefix="vits-piper-es_MX-ald-medium"
with tarfile.open(archive,"r:bz2") as tf:
    for member in tf.getmembers():
        relative=pathlib.PurePosixPath(member.name.lstrip("./"))
        if relative.is_absolute() or ".." in relative.parts or member.issym() or member.islnk():
            raise RuntimeError("Unsafe model path "+member.name)
        # only extract exact model directory
        if not str(relative).startswith(prefix+"/"):continue
        target=root.joinpath(*relative.parts)
        if member.isdir(): target.mkdir(parents=True,exist_ok=True);continue
        if not member.isfile():continue
        target.parent.mkdir(parents=True,exist_ok=True)
        with tf.extractfile(member) as source,target.open("wb") as dest:
            while True:
                buf=source.read(1024*1024)
                if not buf:break
                dest.write(buf)
model=root/prefix
config=sherpa_onnx.OfflineTtsConfig(
 model=sherpa_onnx.OfflineTtsModelConfig(
  vits=sherpa_onnx.OfflineTtsVitsModelConfig(
   model=str(model/"es_MX-ald-medium.onnx"),
   data_dir=str(model/"espeak-ng-data"),
   tokens=str(model/"tokens.txt")),
  num_threads=1))
if not config.validate():raise RuntimeError("Piper config rejected")
tts=sherpa_onnx.OfflineTts(config)
spoken="Hola Wolf, soy CONTROL. Estoy probando una voz masculina local."
audio=tts.generate(text=spoken,sid=0,speed=1.0)
samples=np.asarray(audio.samples)
if samples.size<1000 or audio.sample_rate<8000 or not np.isfinite(samples).all():
 raise RuntimeError("Invalid synthesized audio")
sf.write(str(root/"CONTROL-Ald-MASCULINA-prueba.wav"),samples,audio.sample_rate)
report={"voice":"Ald es_MX masculine only test; not Argentine",
 "sample_rate":audio.sample_rate,
 "duration_seconds":round(samples.size/audio.sample_rate,2),
 "peak":float(np.max(np.abs(samples))),"status":"Linux voice audio generated; not Android APK"}
(root/"synthesis.json").write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
