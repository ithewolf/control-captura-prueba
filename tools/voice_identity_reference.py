#!/usr/bin/env python3
"""CONTROL voice-identity lab: inspect a user-supplied reference locally.

Never upload or commit reference recordings. Designed to run offline on a PC.
The source file must remain on the user's machine; synthetic CI fixture is allowed.
"""
import argparse,hashlib,json,pathlib,subprocess,wave,math,array,sys
p=argparse.ArgumentParser()
p.add_argument("--reference",required=True,help="Private MP3 or WAV reference; not sent online")
p.add_argument("--output",default="build/control-voice-private",help="Local output directory")
p.add_argument("--transcript",help="Optional exact words spoken in the recording")
args=p.parse_args()
source=pathlib.Path(args.reference)
if not source.is_file():p.error("Reference file not found")
out=pathlib.Path(args.output)
out.mkdir(parents=True,exist_ok=True)
wav=out/"control-reference-24k-mono.wav"
cmd=["ffmpeg","-nostdin","-hide_banner","-loglevel","error","-y","-i",str(source),
     "-vn","-ac","1","-ar","24000","-c:a","pcm_s16le",str(wav)]
subprocess.run(cmd,check=True)
with wave.open(str(wav),"rb") as w:
 rate=w.getframerate();frames=w.getnframes();channels=w.getnchannels()
 raw=w.readframes(frames)
samples=array.array("h");samples.frombytes(raw)
rms=math.sqrt(sum(x*x for x in samples)/max(1,len(samples)))/32768
peak=max((abs(x) for x in samples),default=0)/32768
duration=frames/rate
if duration<3 or peak<0.01 or rms<0.001:raise SystemExit("Reference is too short or almost silent")
report={"input_sha256":hashlib.sha256(source.read_bytes()).hexdigest(),
        "duration_seconds":round(duration,3),"sample_rate":rate,
        "channels":channels,"peak":round(peak,4),"rms":round(rms,4),
        "transcript_available":bool(args.transcript),
        "candidate_engines":["OpenVoice V2","XTTS-v2"],
        "status":"REFERENCE PREPARED ONLY; NO CLONED VOICE GENERATED",
        "privacy":"Reference audio stays local; never upload it to public CI"}
(out/"reference-profile.json").write_text(json.dumps(report,indent=2,ensure_ascii=False))
if args.transcript:
 (out/"reference-transcript.txt").write_text(args.transcript,encoding="utf-8")
print(json.dumps(report,indent=2,ensure_ascii=False))
