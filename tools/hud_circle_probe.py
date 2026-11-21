#!/usr/bin/env python3
"""Research prototype: detect Wild Rift PLAYER controls from circular UI shapes.

Requires opencv-python and numpy. NOT a champion/skill detector.
Input is a JPG frame. A count >=10 circles in the bottom-right region is only
a *hypothesis* for a player HUD; never treat it as confirmed without manual
review and a holdout set. Designed for 512px-wide frames. Uses CPU.

Usage: python tools/hud_circle_probe.py path/to/frame.jpg [more frames...]
"""
import json
import sys
import cv2

def assess(path):
    image=cv2.imread(str(path))
    if image is None:raise ValueError(f"Unreadable image: {path}")
    h,w=image.shape[:2]
    if w < 256:
        raise ValueError("Image too small for fixed circle sizes")
    if w != 512:
        image=cv2.resize(image,(512,int(round(h*512/w))))
    h,w=image.shape[:2]
    gray=cv2.cvtColor(image,cv2.COLOR_BGR2GRAY)
    gray=cv2.GaussianBlur(gray,(5,5),1)
    found=cv2.HoughCircles(gray,cv2.HOUGH_GRADIENT,dp=1.3,minDist=20,
                          param1=110,param2=27,minRadius=8,maxRadius=45)
    counts=0
    for x,y,_radius in ([] if found is None else found[0]):
        if x>0.65*w and y>0.55*h:counts+=1
    # Conservative: heuristic suggestion only; no "understands game" claim.
    return {"file":str(path),"lower_right_circles":counts,
            "player_controls_candidate":counts>=10,
            "confidence":"unvalidated research heuristic"}

if __name__=="__main__":
    if len(sys.argv)<2:raise SystemExit(__doc__)
    for name in sys.argv[1:]:
        print(json.dumps(assess(name),ensure_ascii=False))
