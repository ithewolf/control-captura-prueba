#!/usr/bin/env python3
"""Create a reproducible, local sample of Wild Rift gameplay frames.

Requires opencv-python. No videos or images are uploaded anywhere.
Video A can be used for development; reserve video B for validation.
Each frame gets an explicit timestamp. Initial labels are null (unknown),
not fabricated; a human must review them before training/testing a model.

Example:
  python tools/prepare_yasuo_frames.py path/to/yasuo.mp4 --out dataset/yasuo_a --interval 5
"""
import argparse
import json
import pathlib
import cv2

FIELDS=("hud_visible","player_health_visible","minimap_visible","death_screen")
def prepare(video: pathlib.Path, output: pathlib.Path, interval: float, max_frames: int):
    if interval<=0 or max_frames<1:raise ValueError("interval and max_frames must be positive")
    output.mkdir(parents=True,exist_ok=True)
    cap=cv2.VideoCapture(str(video))
    if not cap.isOpened():raise RuntimeError("Could not open video")
    try:
        fps=cap.get(cv2.CAP_PROP_FPS)
        total=cap.get(cv2.CAP_PROP_FRAME_COUNT)
        if fps<=0 or total<=0:raise RuntimeError("Missing video metadata")
        duration=total/fps
        width=int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height=int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        samples=[]
        second=0.0
        while second<duration and len(samples)<max_frames:
            cap.set(cv2.CAP_PROP_POS_MSEC,1000*second)
            ok,frame=cap.read()
            if not ok:
                second+=interval
                continue
            filename=f"frame_{len(samples):04d}_{round(second*1000):08d}ms.jpg"
            if not cv2.imwrite(str(output/filename),frame):
                raise RuntimeError("Could not save frame: "+filename)
            row={"video":video.name,"second":round(second,3),"frame_file":filename,
                 "width":width,"height":height,
                 "perspective":None,"occluded":None}
            row.update({field:None for field in FIELDS})
            samples.append(row)
            second+=interval
        (output/"labels.template.json").write_text(
           json.dumps(samples,ensure_ascii=False,indent=2),encoding="utf-8")
        (output/"README.txt").write_text(
           "Review frame images and fill labels.template.json.\n"
           "Do not score unknown fields as correct. Keep a separate video for evaluation.\n"
           "These are still image samples, not semantic model predictions.\n",
           encoding="utf-8")
        return {"video":video.name,"duration_s":round(duration,2),"samples":len(samples),
                "output":str(output)}
    finally:
        cap.release()

if __name__=="__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("video",type=pathlib.Path)
    parser.add_argument("--out",type=pathlib.Path,required=True)
    parser.add_argument("--interval",type=float,default=5)
    parser.add_argument("--max-frames",type=int,default=300)
    args=parser.parse_args()
    print(json.dumps(prepare(args.video,args.out,args.interval,args.max_frames),
                     ensure_ascii=False,indent=2))
