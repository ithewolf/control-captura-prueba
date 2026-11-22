"""Audit APK contents by type and ABI; packaging and voice inventory only.

Use after assembleDebug. Not a measurement of runtime RAM or Android gameplay FPS.
"""
import json,zipfile
from pathlib import Path

TRAINING_PREFIXES=("lab_hp/","tools/","tests/","dataset/","training/","cvat/")
MODEL_EXTENSIONS=(".pt",".pth",".onnx",".tflite",".safetensors",".ckpt")

def analyze(apk, max_mib=200):
    apk=Path(apk)
    if not apk.is_file() or not zipfile.is_zipfile(apk):
        raise ValueError("valid APK is required")
    if not isinstance(max_mib,(int,float)) or max_mib<=0:
        raise ValueError("invalid size budget")
    with zipfile.ZipFile(apk) as z:
        files=[x for x in z.infolist() if not x.is_dir()]
        bad=z.testzip()
        if bad is not None:raise ValueError("corrupt APK entry")
        counts={"dex":0,"native":0,"assets":0,"other":0}
        abis=set()
        training=[]
        models=[]
        voice=[]
        for f in files:
            name=f.filename
            if name.startswith(TRAINING_PREFIXES):
                training.append(name)
            if name.lower().endswith(MODEL_EXTENSIONS):
                models.append(name)
            if name.startswith("assets/model-es/"):
                voice.append(name)
            if name.startswith("lib/"):
                counts["native"]+=f.compress_size
                parts=name.split("/")
                if len(parts)>2:abis.add(parts[1])
            elif name.startswith("assets/"):counts["assets"]+=f.compress_size
            elif name.endswith(".dex"):counts["dex"]+=f.compress_size
            else:counts["other"]+=f.compress_size
        largest=sorted(files,key=lambda x:x.compress_size,reverse=True)[:10]
    size_mib=apk.stat().st_size/(1024*1024)
    problems=[]
    if size_mib>max_mib:problems.append("APK_OVER_BUDGET")
    if training:problems.append("RESEARCH_FILES_PACKAGED")
    # Voice presence check is intentionally an inventory, not proof Vosk runs.
    if not voice:problems.append("OFFLINE_SPANISH_MODEL_ABSENT")
    return {"status":"PASS" if not problems else "FAIL",
            "apk_mib":round(size_mib,2),"budget_mib":max_mib,
            "sizes_compressed_mib":{k:round(v/1048576,2) for k,v in counts.items()},
            "native_abis":sorted(abis),"voice_assets_count":len(voice),
            "model_files":models,"unwanted_training_files":training,
            "largest":[{"path":x.filename,"mib":round(x.compress_size/1048576,2)} for x in largest],
            "problems":problems,"runtime_ram_measured":False,"gameplay_fps_measured":False}

if __name__=="__main__":
    import argparse
    p=argparse.ArgumentParser();p.add_argument("apk")
    p.add_argument("--budget-mib",type=float,default=200)
    p.add_argument("--output",default="lite-packaging-report.json")
    args=p.parse_args()
    result=analyze(args.apk,args.budget_mib)
    Path(args.output).write_text(json.dumps(result,indent=2),encoding="utf-8")
    print(json.dumps(result,indent=2))
    raise SystemExit(0 if result["status"]=="PASS" else 2)
