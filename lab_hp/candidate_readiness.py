"""Readiness audit for UNLABELED champion video candidates, never claim trained accuracy."""
import json
from pathlib import Path
from collections import Counter
import cv2

def audit_candidates(root):
    root=Path(root).resolve()
    manifests=list(root.glob("*/candidate_manifest.json"))
    out={"sources":len(manifests),"frames":0,"readable":0,"invalid":0,
         "human_reviewed":0,"labels_completed":0,"status":"BLOCKED",
         "issues":[]}
    if not manifests:
        out["issues"].append("NO_CANDIDATE_MANIFESTS")
    seen=set()
    for manifest in manifests:
        rows=json.loads(manifest.read_text(encoding="utf-8"))
        if not isinstance(rows,list):
            out["issues"].append("INVALID_MANIFEST:"+str(manifest));continue
        for row in rows:
            out["frames"]+=1
            if not isinstance(row,dict) or not isinstance(row.get("image"),str):
                out["invalid"]+=1;continue
            path=(root/row["image"]).resolve()
            if not path.is_relative_to(root) or path in seen or not path.is_file():
                out["invalid"]+=1;continue
            seen.add(path)
            frame=cv2.imread(str(path))
            if frame is None or frame.size==0:
                out["invalid"]+=1;continue
            out["readable"]+=1
            if row.get("reviewed") is True:out["human_reviewed"]+=1
            if row.get("label_status")=="CVAT_VERIFIED":out["labels_completed"]+=1
    if out["sources"]<3:out["issues"].append("INSUFFICIENT_INDEPENDENT_VIDEOS_FOR_TRAIN_VAL_TEST")
    if out["invalid"]:out["issues"].append("INVALID_OR_DUPLICATE_IMAGES")
    if out["labels_completed"]!=out["frames"]:out["issues"].append("ANNOTATIONS_INCOMPLETE")
    if out["human_reviewed"]!=out["frames"]:out["issues"].append("REVIEW_INCOMPLETE")
    if out["frames"]==0:out["issues"].append("NO_FRAMES")
    out["status"]="READY_FOR_SPLIT_DESIGN" if not out["issues"] else "BLOCKED"
    out["champion_detection_accuracy"]="NOT_MEASURED"
    return out
if __name__=="__main__":
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument("root");args=parser.parse_args()
    print(json.dumps(audit_candidates(args.root),indent=2,ensure_ascii=False))
