#!/usr/bin/env python3
"""Lab11: evaluate strategy EVENT predictions against independently reviewed labels.
No filename inference, no video upload and no claims that a model saw footage.
Both labels and predictions are required; inspection hints are never ground truth.
"""
import argparse
import csv
import json
from pathlib import Path

EVENTS = {"TEAM_FIGHT", "RETREAT", "GANK", "OBJECTIVE", "LANE_PUSH", "UNKNOWN"}
REQUIRED = {"video_id", "start_s", "end_s", "event"}
TOLERANCE_S = 2.0

def read_csv(path):
    with Path(path).open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))

def normalize(rows):
    result = []
    for row in rows:
        if not REQUIRED.issubset(row):
            raise ValueError("Missing required columns")
        event = row["event"].strip().upper()
        if event not in EVENTS:
            raise ValueError("Unsupported event")
        start, end = float(row["start_s"]), float(row["end_s"])
        if not (0 <= start < end <= 86400):
            raise ValueError("Invalid time window")
        if not row["video_id"].strip():
            raise ValueError("video_id required")
        result.append((row["video_id"].strip(),start,end,event))
    return result

def score(labels=None, predictions=None):
    if labels is None or predictions is None:
        return {"status":"NOT_MEASURED","reason":"Independent reviewed labels and detector predictions required"}
    truth = normalize(labels)
    detected = normalize(predictions)
    if not truth:
        return {"status":"NOT_MEASURED","reason":"No reviewed event windows"}
    used=set()
    tp=fp=0
    for pred in detected:
        candidate=[
            (abs(pred[1]-obs[1])+abs(pred[2]-obs[2]),i)
            for i,obs in enumerate(truth)
            if i not in used and pred[0]==obs[0] and pred[3]==obs[3]
            and abs(pred[1]-obs[1])<=TOLERANCE_S
            and abs(pred[2]-obs[2])<=TOLERANCE_S
        ]
        if candidate:
            used.add(min(candidate)[1])
            tp+=1
        else:
            fp+=1
    fn=len(truth)-len(used)
    return {
        "status":"MEASURED",
        "true_positive":tp, "false_positive":fp, "false_negative":fn,
        "precision":round(tp/(tp+fp),4) if tp+fp else None,
        "recall":round(tp/(tp+fn),4) if tp+fn else None,
        "limitation":"Only reviewed intervals; event recognition != strategic decision quality.",
    }

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--labels",type=Path)
    parser.add_argument("--predictions",type=Path)
    parser.add_argument("--out",type=Path,default=Path("lab11_report.json"))
    args=parser.parse_args()
    if bool(args.labels) != bool(args.predictions):
        raise SystemExit("Supply BOTH --labels and --predictions")
    result=score(read_csv(args.labels) if args.labels else None,
                 read_csv(args.predictions) if args.predictions else None)
    args.out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(result,ensure_ascii=False))

if __name__=="__main__":
    main()
