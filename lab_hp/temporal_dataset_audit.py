"""Verify clip integrity; not an accuracy test for champion or skill recognition."""
import json
from pathlib import Path
import cv2

def audit(root):
    root=Path(root).resolve();reports=[];sources={}
    for manifest in sorted(root.glob("*/clips_manifest.json")):
        folder=manifest.parent
        clips=json.loads(manifest.read_text(encoding="utf-8"))
        if not isinstance(clips,list):raise ValueError("invalid clip manifest")
        for clip in clips:
            errors=[];source=clip.get("source_video");split=clip.get("split","UNASSIGNED")
            if not isinstance(source,str) or not source:errors.append("SOURCE_MISSING")
            if split not in ("UNASSIGNED","train","val","test"):errors.append("SPLIT_INVALID")
            if source and split!="UNASSIGNED":sources.setdefault(source,set()).add(split)
            frames=clip.get("frames")
            if not isinstance(frames,list) or len(frames)<2:
                errors.append("INSUFFICIENT_FRAMES");frames=[]
            last_t=None;seen=set();valid=0
            for f in frames:
                name=f.get("image") if isinstance(f,dict) else None
                ts=f.get("time_s") if isinstance(f,dict) else None
                if not isinstance(name,str):errors.append("INVALID_IMAGE_NAME");continue
                path=(folder/name).resolve()
                if not path.is_relative_to(folder.resolve()) or path in seen:
                    errors.append("INVALID_OR_DUPLICATE_PATH");continue
                seen.add(path)
                if cv2.imread(str(path)) is None:errors.append("UNREADABLE_FRAME")
                else:valid+=1
                if not isinstance(ts,(int,float)) or isinstance(ts,bool) or not 0<=ts<1000000:
                    errors.append("INVALID_TIME")
                elif last_t is not None and ts<=last_t:errors.append("NON_MONOTONIC_TIME")
                if isinstance(ts,(int,float)):last_t=ts
            if clip.get("reviewed") is True and (not frames or any(f.get("reviewed") is not True for f in frames)):
                errors.append("UNVERIFIED_REVIEW")
            reports.append({"clip_id":clip.get("clip_id"),"source_video":source,"frames":len(frames),
                            "readable":valid,"errors":sorted(set(errors)),"reviewed":clip.get("reviewed") is True})
    leaks=[source for source,parts in sources.items() if len(parts)>1]
    failures=sum(bool(r["errors"]) for r in reports)
    return {"status":"FAIL" if failures or leaks or not reports else "PASS","clips":len(reports),
            "frames":sum(x["frames"] for x in reports),"readable":sum(x["readable"] for x in reports),
            "invalid_clips":failures,"source_leakage":leaks,
            "human_reviewed_clips":sum(x["reviewed"] for x in reports),
            "skill_recognition_accuracy":"NOT_MEASURED","reports":reports}
