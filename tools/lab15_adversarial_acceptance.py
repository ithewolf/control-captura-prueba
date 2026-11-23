#!/usr/bin/env python3
"""Fail-closed benchmark gate for authentic gameplay event predictions.

Rejects intro/profile screens, missing independent ground truth, absent detector
provenance, duplicated predictions, unreviewed timestamps, and misplaced claims.
This does NOT identify pixels or generate detector predictions.
"""
import argparse
import csv
import json
import math
from pathlib import Path

EVENTS={"TEAM_FIGHT","RETREAT","GANK","OBJECTIVE","LANE_PUSH","NO_GAMEPLAY"}
MATCH_TOLERANCE_S=2.0

def parse(rows, predicted=False):
    clean=[]
    for row in rows:
        for k in ("video_id","start_s","end_s","event"):
            if k not in row: raise ValueError("missing column "+k)
        vid=row["video_id"].strip()
        label=row["event"].strip().upper()
        start,end=float(row["start_s"]),float(row["end_s"])
        if not vid or label not in EVENTS or not math.isfinite(start) or not math.isfinite(end) or not 0<=start<end<=86400:
            raise ValueError("invalid event, time or video id")
        if predicted:
            model=row.get("model_id","").strip()
            if not model or model.upper() in ("UNSPECIFIED","MANUAL","ORACLE","GROUND_TRUTH"):
                raise ValueError("independent model_id required")
            confidence=float(row.get("confidence","nan"))
            if not math.isfinite(confidence) or not 0<=confidence<=1:
                raise ValueError("invalid confidence")
        else:
            model=None;confidence=None
        clean.append({"video_id":vid,"start_s":start,"end_s":end,"event":label,
                      "model_id":model,"confidence":confidence})
    return clean

def overlap(p,l,tol=MATCH_TOLERANCE_S):
    return p["video_id"]==l["video_id"] and abs(p["start_s"]-l["start_s"])<=tol and abs(p["end_s"]-l["end_s"])<=tol

def evaluate(labels=None,predictions=None,source_verified=False,independent=False):
    failures=[]
    if not source_verified: failures.append("GAME_AND_VIEW_NOT_VERIFIED")
    if not independent: failures.append("LABELS_AND_PREDICTIONS_NOT_PROVEN_INDEPENDENT")
    if labels is None or predictions is None:
        failures.append("MISSING_HUMAN_LABELS_OR_DETECTOR_PREDICTIONS")
        return {"status":"BLOCKED","failures":failures,"metrics":"NOT_MEASURED",
                "false_positive_on_no_gameplay":"NOT_MEASURED"}
    truth=parse(labels)
    preds=parse(predictions,True)
    if not truth:
        failures.append("ZERO_REVIEWED_INTERVALS")
    if not preds:
        failures.append("ZERO_DETECTOR_PREDICTIONS")
    if failures:
        return {"status":"BLOCKED","failures":failures,"metrics":"NOT_MEASURED",
                "false_positive_on_no_gameplay":"NOT_MEASURED"}
    keys=[(p["video_id"],p["start_s"],p["end_s"],p["event"]) for p in preds]
    if len(keys)!=len(set(keys)): failures.append("DUPLICATED_PREDICTIONS")
    if len({x["model_id"] for x in preds})!=1: failures.append("MULTIPLE_MODELS_WITHOUT_SEPARATE_SCORES")
    reviewed=[l for l in truth if l["event"]!="NO_GAMEPLAY"]
    negatives=[l for l in truth if l["event"]=="NO_GAMEPLAY"]
    if not negatives: failures.append("MISSING_NON_GAMEPLAY_NEGATIVE_CONTROL")
    if not reviewed: failures.append("MISSING_POSITIVE_GAMEPLAY_GROUND_TRUTH")
    if failures:
        return {"status":"BLOCKED","failures":failures,"metrics":"NOT_MEASURED",
                "false_positive_on_no_gameplay":"NOT_MEASURED"}
    false_gameplay=[]
    unreviewed=[]
    remaining=[]
    for p in preds:
        if any(overlap(p,n) for n in negatives):
            if p["event"]!="NO_GAMEPLAY": false_gameplay.append(p)
        elif not any(overlap(p,t) for t in reviewed):
            unreviewed.append(p)
        else:
            remaining.append(p)
    used=set()
    tp=fp=0
    for p in remaining:
        matches=[(abs(p["start_s"]-t["start_s"])+abs(p["end_s"]-t["end_s"]),i)
                 for i,t in enumerate(reviewed) if i not in used and p["event"]==t["event"] and overlap(p,t)]
        if matches:
            used.add(min(matches)[1]);tp+=1
        else: fp+=1
    fn=len(reviewed)-len(used)
    # Hold invalid/unreviewed predictions visible without guessing labels.
    return {"status":"EVALUATED_WITH_FAILURES" if false_gameplay or unreviewed or fp or fn else "EVALUATED",
      "failures":(["FALSE_GAMEPLAY_TRIGGER_ON_NON_GAMEPLAY"] if false_gameplay else [])
                 +(["PREDICTION_OUTSIDE_REVIEWED_INTERVALS"] if unreviewed else []),
      "metrics":{"tp":tp,"fp_reviewed":fp,"fn":fn,
                 "precision_reviewed":round(tp/(tp+fp),4) if tp+fp else None,
                 "recall_reviewed":round(tp/(tp+fn),4) if tp+fn else None},
      "false_positive_on_no_gameplay":len(false_gameplay),
      "predictions_outside_reviewed_intervals":len(unreviewed),
      "notes":"Metrics only on reviewed windows; NO_GAMEPLAY controls scored separately. No video bytes processed."}

def load(path):
    with Path(path).open(encoding="utf-8",newline="") as f:return list(csv.DictReader(f))

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--labels",type=Path)
    ap.add_argument("--predictions",type=Path)
    ap.add_argument("--source-verified",action="store_true")
    ap.add_argument("--independent",action="store_true")
    ap.add_argument("--out",type=Path,default=Path("lab15_acceptance.json"))
    args=ap.parse_args()
    result=evaluate(load(args.labels) if args.labels else None,
                    load(args.predictions) if args.predictions else None,
                    args.source_verified,args.independent)
    args.out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps(result))
    if result["status"]!="EVALUATED":raise SystemExit(1)

if __name__=="__main__":main()
