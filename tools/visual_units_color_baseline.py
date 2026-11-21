#!/usr/bin/env python3
"""CONTROL visual baseline: color line candidates are NOT ally/enemy recognition.
Usage: python tools/visual_units_color_baseline.py --game yasuo|warwick /path/video.mp4
Requires opencv-python and numpy. Does not publish video or require internet.
"""
import argparse
import cv2
import json
import numpy as np

def roi_ok(game,x,y,width,height):
    if game=="yasuo" and ((x<width*.19 and y<height*.34) or y<height*.095 or y>height*.91):
        return False
    if game=="warwick" and ((x>width*.78 and y>height*.53) or y<height*.07 or y>height*.83):
        return False
    return True

def candidates(frame,game):
    h,w=frame.shape[:2]
    hsv=cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)
    masks={
        "red":cv2.inRange(hsv,(0,100,70),(12,255,255))|cv2.inRange(hsv,(170,100,70),(179,255,255)),
        "blue":cv2.inRange(hsv,(90,95,80),(135,255,255)),
        "green":cv2.inRange(hsv,(34,100,60),(86,255,255))}
    results=[]
    for color,mask in masks.items():
        mask=cv2.morphologyEx(mask,cv2.MORPH_OPEN,cv2.getStructuringElement(cv2.MORPH_RECT,(13,1)))
        _,labels,stats,_=cv2.connectedComponentsWithStats(mask)
        for x,y,bw,bh,area in stats[1:]:
            if 16<=bw<=100 and bh<=9 and bw>4*bh and area>=14 and roi_ok(game,int(x),int(y),w,h):
                results.append({"color":color,"x":int(x),"y":int(y),"w":int(bw),"h":int(bh)})
    return results

def scan(video,game,interval):
    cap=cv2.VideoCapture(video)
    if not cap.isOpened():raise RuntimeError("Cannot open input video")
    fps=cap.get(cv2.CAP_PROP_FPS)
    n=cap.get(cv2.CAP_PROP_FRAME_COUNT)
    if fps<=0:raise RuntimeError("Invalid frame rate")
    samples=[]
    for sec in range(20,int(n/fps),interval):
        cap.set(cv2.CAP_PROP_POS_MSEC,sec*1000)
        ok,fr=cap.read()
        if not ok:continue
        boxes=candidates(fr,game)
        samples.append({"second":sec,"segments":{name:sum(x["color"]==name for x in boxes)
                      for name in ("red","blue","green")}})
    cap.release()
    return {"video":video,"game":game,"samples":samples,
            "totals":{c:sum(it["segments"][c] for it in samples) for c in ("red","blue","green")},
            "semantic_recognition":"NOT_IMPLEMENTED",
            "warning":"Visual colors can be UI, jungle monsters, effects or terrain. Never send these candidates as enemyNear/allyNear."}

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--game",required=True,choices=["yasuo","warwick"])
    p.add_argument("--interval",type=int,default=10)
    p.add_argument("video")
    a=p.parse_args()
    if a.interval<1:raise SystemExit("interval must be >= 1")
    print(json.dumps(scan(a.video,a.game,a.interval),ensure_ascii=False,indent=2))
