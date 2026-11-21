#!/usr/bin/env python3
"""Flag near-duplicate frames, including across training and holdout videos.

Requires Pillow (PIL). Perceptual dHash is used as a *leakage warning*, never
as ground truth. A small Hamming distance may occur in distinct scenes; inspect
flagged pairs before removing or grouping them. Distinct scenes can share
similar hashes, and large image changes can conceal repeated gameplay content.

Usage:
  python tools/check_frame_leakage.py dataset/train/labels.template.json dataset/test/labels.template.json --threshold 6
"""
import argparse
import json
from pathlib import Path
from PIL import Image

def dhash(path):
    with Image.open(path) as im:
        gray=im.convert("L").resize((9,8),Image.Resampling.LANCZOS)
        values=list(gray.getdata())
    bits=0
    for y in range(8):
        for x in range(8):
            bits=(bits<<1)|int(values[y*9+x]>values[y*9+x+1])
    return bits

def scan(train_json,test_json,threshold=6):
    if not 0<=threshold<=64:raise ValueError("threshold must be between 0 and 64")
    entries=[]
    for split,source in (("train",Path(train_json)),("holdout",Path(test_json))):
        rows=json.loads(source.read_text(encoding="utf-8"))
        for row in rows:
            path=source.parent / row["frame_file"]
            entries.append({"split":split,"file":str(path),
                            "video":row["video"],"second":row["second"],
                            "hash":dhash(path)})
    pairs=[]
    for i,a in enumerate(entries):
        for b in entries[i+1:]:
            # Only flag cross-split comparisons, which can invalidate holdout.
            if a["split"]==b["split"]:continue
            distance=(a["hash"]^b["hash"]).bit_count()
            if distance<=threshold:
                pairs.append({"train_file":a["file"],"test_file":b["file"],
                              "train_video":a["video"],"test_video":b["video"],
                              "distance":distance})
    return {"train_frames":sum(x["split"]=="train" for x in entries),
            "holdout_frames":sum(x["split"]=="holdout" for x in entries),
            "threshold":threshold,"suspected_leakage_pairs":pairs,
            "clean_of_detected_near_duplicates":not bool(pairs)}

if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("training_json");p.add_argument("holdout_json")
    p.add_argument("--threshold",type=int,default=6)
    a=p.parse_args()
    result=scan(a.training_json,a.holdout_json,a.threshold)
    print(json.dumps(result,ensure_ascii=False,indent=2))
    if result["suspected_leakage_pairs"]:raise SystemExit(2)
