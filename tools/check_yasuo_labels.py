#!/usr/bin/env python3
"""Validate two-video Yasuo evaluation protocol, without claiming understanding.

Labels must have video, second and at least one boolean target. The same
video must never appear in train and holdout. No unlabelled values count as
ground truth, and duplicated timestamps are rejected.
"""
import argparse
import json
from collections import Counter
from pathlib import Path

FIELDS=("hud_visible","player_health_visible","minimap_visible","death_screen")

def validate(rows, *, require_labels=True):
    if not isinstance(rows,list) or not rows:
        raise ValueError("Expected non-empty JSON array")
    seen=set(); sources=set(); labeled=Counter()
    for i,row in enumerate(rows):
        if not isinstance(row,dict):
            raise ValueError(f"Row {i} is not an object")
        source=row.get("video")
        second=row.get("second")
        if not isinstance(source,str) or not source.strip():
            raise ValueError(f"Row {i}: missing video name")
        if isinstance(second,bool) or not isinstance(second,(int,float)) or second<0:
            raise ValueError(f"Row {i}: invalid second")
        ident=(source,round(second,3))
        if ident in seen:
            raise ValueError(f"Duplicated video and second: {ident}")
        seen.add(ident); sources.add(source)
        for field in FIELDS:
            value=row.get(field)
            if value is not None and not isinstance(value,bool):
                raise ValueError(f"Row {i}: {field} must be bool or null")
            if isinstance(value,bool):labeled[field]+=1
    if require_labels and not any(labeled.values()):
        raise ValueError("No verified human labels; cannot claim vision accuracy")
    return {"samples":len(rows),"videos":sorted(sources),
            "labeled_per_field":dict(labeled)}

def check_split(training,holdout):
    train=validate(training);test=validate(holdout)
    overlap=set(train["videos"]) & set(test["videos"])
    if overlap:raise ValueError("Data leakage: same video in train and holdout: "+str(sorted(overlap)))
    return {"train":train,"holdout":test,"separate_videos":True}

if __name__=="__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("train",type=Path)
    parser.add_argument("holdout",type=Path)
    args=parser.parse_args()
    output=check_split(json.loads(args.train.read_text(encoding="utf-8")),
                       json.loads(args.holdout.read_text(encoding="utf-8")))
    print(json.dumps(output,indent=2,ensure_ascii=False))
