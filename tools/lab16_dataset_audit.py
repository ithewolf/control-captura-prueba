#!/usr/bin/env python3
"""CONTROL Lab16: guard a human-annotated Wild Rift image dataset.

No detector training. No guessed annotations. Original frames stay outside Git.
"""
import argparse, csv, json, math
from collections import Counter
from pathlib import Path

CLASSES=("SELF","ALLY_CHAMPION","ENEMY_CHAMPION","ALLY_MINION","ENEMY_MINION",
         "TOWER","DRAGON","BARON")
def audit(manifest_path,images_root,labels_root=None):
    with Path(manifest_path).open(encoding="utf-8",newline="") as f: items=list(csv.DictReader(f))
    problems=[]; stats=Counter(); seen=set(); video_splits={}
    for row in items:
        name=row.get("image","")
        if not name or Path(name).name!=name or name in seen:
            problems.append("invalid or duplicate filename: "+name);continue
        seen.add(name)
        split=row.get("split")
        if split not in ("development","holdout"):problems.append(name+": invalid split")
        video_id=row.get("video_id","").strip()
        if not video_id:problems.append(name+": video_id missing")
        elif video_id in video_splits and video_splits[video_id]!=split:
            problems.append(name+": same source video in development and holdout (DATA LEAKAGE)")
        else:video_splits[video_id]=split
        path=Path(images_root)/name
        if not path.is_file():problems.append(name+": missing image")
        if row.get("game_verified")!="YES":
            problems.append(name+": source game/version/view is not human verified")
        if row.get("scene_label") in ("","UNREVIEWED",None):
            problems.append(name+": no independently reviewed scene label")
        if labels_root is None:continue
        label=Path(labels_root)/(Path(name).stem+".txt")
        if not label.is_file():
            problems.append(name+": missing bounding-box label");continue
        for line in label.read_text(encoding="utf-8").splitlines():
            bits=line.split()
            if len(bits)!=5:
                problems.append(name+": invalid YOLO box columns");continue
            try:
                cls=int(bits[0]);coords=[float(x) for x in bits[1:]]
                if cls<0 or cls>=len(CLASSES) or not all(math.isfinite(x) and 0<=x<=1 for x in coords) or coords[2]<=0 or coords[3]<=0:
                    raise ValueError
            except ValueError:problems.append(name+": invalid class/coordinates")
            stats["boxes"]+=1
        stats["reviewed_label_files"]+=1
    if not items:problems.append("empty manifest")
    if not {"development","holdout"}.issubset({r.get("split") for r in items}):
        problems.append("missing independent holdout split")
    return {"status":"READY_FOR_MODEL_EVALUATION" if not problems and labels_root else "BLOCKED",
        "images_listed":len(items),"classes":list(CLASSES),"annotation_files":stats["reviewed_label_files"],
        "boxes":stats["boxes"],"issues":problems,
        "recognition_accuracy":"NOT_MEASURED",
        "note":"Dataset readiness is not recognition. Evaluate unseen video and models separately."}
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--manifest",type=Path,required=True)
    p.add_argument("--images",type=Path,required=True)
    p.add_argument("--labels",type=Path)
    p.add_argument("--out",type=Path,default=Path("lab16_readiness.json"))
    args=p.parse_args()
    data=audit(args.manifest,args.images,args.labels)
    args.out.write_text(json.dumps(data,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({"status":data["status"],"images":data["images_listed"],"issues":len(data["issues"])}))
    if data["status"]!="READY_FOR_MODEL_EVALUATION":raise SystemExit(2)
if __name__=="__main__":main()
