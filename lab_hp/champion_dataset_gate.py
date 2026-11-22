"""Offline audit of human-reviewed champion labels, separate from HP labels."""
import math
from pathlib import Path
from collections import Counter,defaultdict

CHAMPIONS=("fiora","sett","syndra","yasuo","warwick")
SPLITS=("train","val","test")

def labels_ok(text):
    counts=Counter()
    for n,line in enumerate(text.splitlines(),1):
        if not line.strip():continue
        p=line.split()
        if len(p)!=5:raise ValueError(f"line {n}: five YOLO columns required")
        try: cls=int(p[0]); xc,yc,w,h=map(float,p[1:])
        except (ValueError,OverflowError) as e:raise ValueError("invalid label") from e
        if not (0<=cls<len(CHAMPIONS) and all(map(math.isfinite,(xc,yc,w,h))) and w>0 and h>0
                and xc-w/2>=-1e-6 and xc+w/2<=1+1e-6 and yc-h/2>=-1e-6 and yc+h/2<=1+1e-6):
            raise ValueError(f"line {n}: invalid box or class")
        counts[CHAMPIONS[cls]]+=1
    return counts

def audit(manifest,root):
    if not isinstance(manifest,list) or not manifest:raise ValueError("nonempty manifest required")
    root=Path(root).resolve();images=set();sources=defaultdict(set);splits=Counter();labels=Counter()
    for index,row in enumerate(manifest):
        if not isinstance(row,dict):raise ValueError("invalid row")
        for k in ("image","labels","source_video","scene_id","split"):
            if not isinstance(row.get(k),str) or not row[k].strip():raise ValueError(f"row {index}: missing {k}")
        if row["split"] not in SPLITS or row.get("reviewed") is not True or type(row.get("negative")) is not bool:
            raise ValueError("invalid split or unreviewed label")
        image=(root/row["image"]).resolve();label=(root/row["labels"]).resolve()
        if not image.is_relative_to(root) or not label.is_relative_to(root) or image in images:
            raise ValueError("unsafe or duplicate image")
        if not image.is_file() or not label.is_file():raise ValueError("missing asset")
        images.add(image)
        count=labels_ok(label.read_text(encoding="utf-8"))
        if bool(sum(count.values()))==row["negative"]:raise ValueError("negative flag contradicts labels")
        sources[row["source_video"]].add(row["split"])
        splits[row["split"]]+=1;labels.update(count)
    leaks=[source for source,parts in sources.items() if len(parts)>1]
    return {"status":"FAIL" if leaks else "PASS","source_leakage":leaks,
            "images":dict(splits),"labels":dict(labels),"detector_accuracy":"NOT_MEASURED"}
