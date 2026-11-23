#!/usr/bin/env python3
"""Compute temporal image-change signals from actual decoded video frames.
High change DOES NOT imply combat, ganks, tactics or learning.
No raw video or frames are uploaded to GitHub by this script.
"""
import argparse, json, math
from pathlib import Path

def scan(path, step=2.0, max_seconds=180.0):
    import cv2
    cap = cv2.VideoCapture(str(path))
    if not cap.isOpened(): raise ValueError("Could not open video")
    fps = cap.get(cv2.CAP_PROP_FPS)
    count = cap.get(cv2.CAP_PROP_FRAME_COUNT)
    if not math.isfinite(fps) or fps <= 0: raise ValueError("Invalid FPS")
    duration = count / fps
    records=[]; prev=None; ts=0.0
    while ts < min(duration-0.1,max_seconds):
        cap.set(cv2.CAP_PROP_POS_MSEC,ts*1000)
        ok,frame=cap.read()
        if not ok: break
        h,w=frame.shape[:2]
        crop=frame[int(h*.16):int(h*.85), int(w*.16):int(w*.84)]
        gray=cv2.cvtColor(cv2.resize(crop,(224,126)),cv2.COLOR_BGR2GRAY)
        gray=cv2.GaussianBlur(gray,(5,5),0)
        delta=None if prev is None else float(cv2.absdiff(gray,prev).mean())
        records.append({"second":round(ts,2),"mean_frame_change":round(delta,4) if delta is not None else None})
        prev=gray
        ts+=step
    cap.release()
    values=[r["mean_frame_change"] for r in records if r["mean_frame_change"] is not None]
    ranked=sorted((r for r in records if r["mean_frame_change"] is not None),key=lambda r:-r["mean_frame_change"])
    return {"status":"VISUAL_ACTIVITY_MEASURED","game_events_status":"NOT_MEASURED",
      "source_kind":"user_provided_video","fps":round(fps,3),"duration_s":round(duration,3),
      "sample_step_s":step,"samples":len(records),
      "mean_visual_change":round(sum(values)/len(values),4) if values else None,
      "top_activity_windows":ranked[:10],"records":records,
      "limitations":[
        "High frame change may mean camera movement, edits, effects or combat, not event classification",
        "This is not champion recognition, strategy recognition or evidence of learned tactics"]}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--video",type=Path,required=True)
    parser.add_argument("--out",type=Path,required=True)
    parser.add_argument("--step",type=float,default=2.0)
    args=parser.parse_args()
    if not 0 < args.step <= 60: raise SystemExit("step must be between 0 and 60 seconds")
    report=scan(args.video,args.step)
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:v for k,v in report.items() if k!="records"},ensure_ascii=False))
if __name__=="__main__":main()
