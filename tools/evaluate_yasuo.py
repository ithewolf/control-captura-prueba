#!/usr/bin/env python3
"""Offline evaluation of Wild Rift HUD recognizer predictions against human labels.

Input JSON arrays have entries with "video", "second" and labels:
 hud_visible, player_health_visible, minimap_visible, death_screen (boolean or null).
 Missing labels are excluded, never counted correct. This script does not
 *recognize* the HUD; it measures output produced by a separate recognizer.

Usage: python tools/evaluate_yasuo.py labels.json predictions.json
"""
import json, sys
from collections import defaultdict

FIELDS=("hud_visible","player_health_visible","minimap_visible","death_screen")

def key(row):
    return (str(row["video"]),round(float(row["second"]),2))

def evaluate(gold,predictions):
    predicted={key(row):row for row in predictions}
    output={"samples":len(gold),"matched_predictions":0,"fields":{}}
    for field in FIELDS:
        counts=defaultdict(int)
        for row in gold:
            truth=row.get(field,None)
            if truth is None: continue
            item=predicted.get(key(row),{})
            guess=item.get(field,None)
            counts["labeled"]+=1
            if guess is None:
                counts["abstained"]+=1
                continue
            if not isinstance(guess,bool) or not isinstance(truth,bool):
                raise ValueError("Expected boolean or null: "+field)
            if guess==truth:counts["correct"]+=1
            if guess and truth:counts["tp"]+=1
            if guess and not truth:counts["fp"]+=1
            if not guess and truth:counts["fn"]+=1
            if not guess and not truth:counts["tn"]+=1
        n=counts["labeled"]
        decided=n-counts["abstained"]
        counts["coverage"]=round(decided/n,4) if n else None
        counts["accuracy_on_decided"]=round(counts["correct"]/decided,4) if decided else None
        counts["precision"]=round(counts["tp"]/(counts["tp"]+counts["fp"]),4) if counts["tp"]+counts["fp"] else None
        counts["recall"]=round(counts["tp"]/(counts["tp"]+counts["fn"]),4) if counts["tp"]+counts["fn"] else None
        output["fields"][field]=dict(counts)
    output["matched_predictions"]=sum(key(row) in predicted for row in gold)
    return output

if __name__=="__main__":
    if len(sys.argv)!=3:sys.exit(__doc__)
    with open(sys.argv[1],encoding="utf-8") as f:gold=json.load(f)
    with open(sys.argv[2],encoding="utf-8") as f:pred=json.load(f)
    print(json.dumps(evaluate(gold,pred),indent=2,ensure_ascii=False))
