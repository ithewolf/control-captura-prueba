#!/usr/bin/env python3
"""Lab5: local OpenVoice V2 + MeloTTS Spanish candidate for CONTROL.

This runs only on a private computer with explicitly installed open-source
dependencies/checkpoints. It never uploads the reference recording.
Do not use a voice you lack permission to synthesize.
"""
import argparse,json,pathlib,time,hashlib,sys

PHRASES=[
 "Wolf, retrocedé. Todavía no es seguro entrar.",
 "Está bien, ve por él.",
 "Prestá atención al minimapa y cuidá tu posición.",
 "Estoy analizando lo que veo, pero todavía puedo equivocarme.",
 "Tenemos que jugar con calma. Esperá mi señal."
]
def parse():
 p=argparse.ArgumentParser()
 p.add_argument("--reference",required=True,help="Private user-authorized MP3/WAV")
 p.add_argument("--checkpoints",required=True,help="OpenVoice checkpoint_v2 folder")
 p.add_argument("--out",default="build/lab5-private-samples")
 p.add_argument("--device",choices=["cpu","cuda:0"],default="cpu")
 p.add_argument("--only-preflight",action="store_true")
 return p.parse_args()

def main():
 a=parse()
 original=pathlib.Path(a.reference)
 ckpt=pathlib.Path(a.checkpoints)
 if not original.is_file():raise SystemExit("Missing authorized voice reference")
 converter=ckpt/"converter"
 need=[converter/"config.json",converter/"checkpoint.pth",ckpt/"base_speakers"/"ses"/"es.pth"]
 missing=[str(p) for p in need if not p.is_file()]
 if missing:raise SystemExit("Missing official OpenVoice V2 checkpoint files: "+str(missing))
 out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=True)
 if a.only_preflight:
  print("Lab5 OpenVoice V2: references and checkpoints present; no model inference");return
 import torch
 from openvoice import se_extractor
 from openvoice.api import ToneColorConverter
 from melo.api import TTS
 started=time.monotonic()
 voice_converter=ToneColorConverter(str(converter/"config.json"),device=a.device)
 voice_converter.load_ckpt(str(converter/"checkpoint.pth"))
 target_se,_=se_extractor.get_se(str(original),voice_converter,vad=True)
 speaker=TTS(language="ES",device=a.device)
 speaker_ids=speaker.hps.data.spk2id
 if not speaker_ids:raise RuntimeError("MeloTTS Spanish speakers unavailable")
 # Official demo uses language tag ES. Confirm matching embedding exists.
 speaker_key=next((k for k in speaker_ids if (ckpt/"base_speakers"/"ses"/(k.lower().replace("_","-")+".pth")).is_file()),None)
 if speaker_key is None:raise RuntimeError("No Spanish speaker embedding matches MeloTTS speaker id")
 source_se=torch.load(str(ckpt/"base_speakers"/"ses"/(speaker_key.lower().replace("_","-")+".pth")),map_location=a.device,weights_only=True)
 speaker_id=speaker_ids[speaker_key]
 results=[]
 for n,phrase in enumerate(PHRASES,1):
  start=time.monotonic()
  raw=out/(f"base-{n:02d}.wav")
  final=out/(f"CONTROL-voz-prueba-{n:02d}.wav")
  speaker.tts_to_file(phrase,speaker_id,str(raw),speed=1.0)
  voice_converter.convert(audio_src_path=str(raw),src_se=source_se,tgt_se=target_se,output_path=str(final),message="@MyShell")
  if not final.is_file() or final.stat().st_size<1000:
   raise RuntimeError("Cloned WAV missing or empty: "+str(final))
  # Do not save or send source/reference files to CI.
  results.append({"id":n,"text":phrase,"output":str(final),"bytes":final.stat().st_size,"elapsed_s":round(time.monotonic()-start,2)})
 report={"engine":"OpenVoice V2 + MeloTTS ES","voice":"CONTROL authorized reference",
   "reference_sha256":hashlib.sha256(original.read_bytes()).hexdigest(),
   "total_elapsed_s":round(time.monotonic()-started,2),
   "outputs":results,"status":"SYNTHESIZED: subjective voice fidelity still requires owner listening",
   "warning":"DO NOT confuse candidate audio with the final approved CONTROL voice"}
 (out/"trial-report.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
 print(json.dumps(report,ensure_ascii=False,indent=2))
if __name__=="__main__":main()
