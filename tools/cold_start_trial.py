#!/usr/bin/env python3
"""CONTROL vision cold-start trial orchestrator.

Keeps the trained recognizer/model unchanged while resetting session memory.
Safely resets only an explicitly named temporary workspace; never erases
training examples, learned model weights, source videos, or personal memories.
This harness creates an empty session folder and a manifest recording the
fixed model SHA256, so repeated trials can be compared reproducibly.

Example:
 python tools/cold_start_trial.py --model model.tflite \
   --workspace /tmp/control_cold_start --trial yasuoA_01 \
   --video yasuo.mp4 --split replay
"""
import argparse, hashlib, json, pathlib, re, shutil, time

def sha256(path):
    digest=hashlib.sha256()
    with open(path,"rb") as file:
        for chunk in iter(lambda:file.read(1024*1024),b""):digest.update(chunk)
    return digest.hexdigest()

def initialize(model,workspace,trial,video,split,reset=False):
    model=pathlib.Path(model).resolve(strict=True)
    video=pathlib.Path(video).resolve(strict=True)
    workspace=pathlib.Path(workspace).resolve()
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}",trial):
        raise ValueError("Invalid trial name")
    if split not in ("replay","holdout"):
        raise ValueError("split must be replay or holdout")
    if workspace==model.parent or workspace==video.parent:
        raise ValueError("Workspace cannot be the model or video directory")
    root=workspace/trial
    if root.exists():
        if not reset:raise FileExistsError("Trial exists: use a new trial name or --reset-session")
        marker=root/"session_marker.json"
        if not marker.is_file():raise ValueError("Refusing reset of folder without session marker")
        previous=json.loads(marker.read_text(encoding="utf-8"))
        if previous.get("trial")!=trial or previous.get("generated_by")!="cold_start_trial":
            raise ValueError("Refusing reset: unknown session marker")
        # Never accept model or source video inside the reset folder.
        if model.is_relative_to(root) or video.is_relative_to(root):
            raise ValueError("Refusing to remove model or video")
        shutil.rmtree(root)
    root.mkdir(parents=True,exist_ok=False)
    marker={"generated_by":"cold_start_trial","trial":trial,"split":split,
            "model_path":str(model),"model_sha256":sha256(model),
            "video_path":str(video),"created_at_unix":int(time.time()),
            "session_memory_reset":True,
            "recognizer_executed":False,
            "gameplay_understanding_verified":False}
    (root/"session_marker.json").write_text(json.dumps(marker,ensure_ascii=False,indent=2),encoding="utf-8")
    (root/"session_cache").mkdir()
    (root/"observations.json").write_text("[]\n",encoding="utf-8")
    return marker

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model",required=True)
    parser.add_argument("--workspace",required=True)
    parser.add_argument("--trial",required=True)
    parser.add_argument("--video",required=True)
    parser.add_argument("--split",required=True,choices=("replay","holdout"))
    parser.add_argument("--reset-session",action="store_true")
    args=parser.parse_args()
    print(json.dumps(initialize(args.model,args.workspace,args.trial,args.video,args.split,args.reset_session),
                     ensure_ascii=False,indent=2))
if __name__=="__main__":main()
