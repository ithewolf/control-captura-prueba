#!/usr/bin/env python3
"""Check five independent CONTROL voice-clone trials before any app integration.

Measures basic audio integrity, not voice identity. Keep generated clips private.
"""
import argparse,json,math,pathlib,subprocess,wave,csv,hashlib
p=argparse.ArgumentParser()
p.add_argument("--samples",required=True)
p.add_argument("--output",default=None)
args=p.parse_args()
folder=pathlib.Path(args.samples)
expected=[folder/f"CONTROL-voz-prueba-{n:02d}.wav" for n in range(1,6)]
missing=[str(x) for x in expected if not x.is_file()]
if missing:raise SystemExit("Still no five generated phrases: "+", ".join(missing))
results=[]
for path in expected:
 with wave.open(str(path),"rb") as w:
  if w.getcomptype()!="NONE":raise RuntimeError("Unsupported compressed WAV "+str(path))
  rate=w.getframerate();count=w.getnframes();channels=w.getnchannels();width=w.getsampwidth()
  duration=count/rate
  if rate<16000 or channels not in (1,2) or width not in (2,4) or duration<0.7 or duration>45:
   raise RuntimeError("Invalid sample audio metadata: "+str(path))
  raw=w.readframes(count)
  if width==2:
   import array
   samples=array.array("h");samples.frombytes(raw)
   rms=math.sqrt(sum(v*v for v in samples)/max(1,len(samples)))/32768
   peak=max((abs(v) for v in samples),default=0)/32768
  else:
   rms=peak=None
  if rms is not None and (rms<0.001 or peak>1.0001):raise RuntimeError("Silent/invalid sample "+str(path))
  results.append({"file":path.name,"seconds":round(duration,2),"rate":rate,
                  "channels":channels,"rms":round(rms,4) if rms is not None else None,
                  "sha256":hashlib.sha256(path.read_bytes()).hexdigest()})
if len({item["sha256"] for item in results})!=len(results):
 raise RuntimeError("Duplicated WAV content: five distinct generated files required")
output=pathlib.Path(args.output or str(folder/"acceptance"))
output.mkdir(parents=True,exist_ok=True)
report={"status":"Technical WAV integrity PASSED; voice identity NOT assessed",
"voice_approved":False,"ready_for_android":False,
"must_listen_for":["original CONTROL voice resemblance","Spanish intelligibility",
"Argentine intonation","consistent timbre across all 5 phrases","non-repetitive speech"],
"files":results}
(output/"technical-report.json").write_text(json.dumps(report,indent=2,ensure_ascii=False))
with (output/"wolf-review.csv").open("w",encoding="utf-8",newline="") as f:
 writer=csv.writer(f)
 writer.writerow(["archivo","timbre_parecido_1_a_5","pronunciacion_1_a_5",
 "acento_argentino_1_a_5","naturalidad_1_a_5","aprobado_si_no","comentario"])
 for item in results:writer.writerow([item["file"],"","","","","",""])
print(json.dumps(report,indent=2,ensure_ascii=False))
