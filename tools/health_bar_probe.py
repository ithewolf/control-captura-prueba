#!/usr/bin/env python3
"""Offline candidate green horizontal bar detector for Wild Rift frames.

CPU-only research heuristic, not a trained HUD detector. A bright horizontal
line may be an HP bar, jungle creature, scenery or an effect. NEVER infer
HP percentage, team, champion or game state from this output alone.
"""
import argparse
import json
from pathlib import Path
import cv2
import numpy as np


def detect(image):
    if image is None or image.ndim != 3:
        raise ValueError('expected BGR image')
    h, w = image.shape[:2]
    if w < 256 or h < 140:
        raise ValueError('frame too small')
    if w != 512:
        image = cv2.resize(image, (512, round(h * 512 / w)))
        h, w = image.shape[:2]
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
    mask = cv2.inRange(hsv, (38, 75, 65), (87, 255, 255))
    mask[:int(.15*h), :] = 0
    mask[int(.82*h):, :] = 0
    mask[:, :int(.22*w)] = 0
    mask[:, int(.82*w):] = 0
    joined = cv2.morphologyEx(mask, cv2.MORPH_CLOSE,
                              cv2.getStructuringElement(cv2.MORPH_RECT, (7, 1)))
    n, labels, stats, _ = cv2.connectedComponentsWithStats(joined)
    candidates=[]
    for x,y,bw,bh,area in stats[1:]:
        x,y,bw,bh,area=map(int,(x,y,bw,bh,area))
        ratio=bw/max(1,bh)
        if not (14<=bw<=95 and 1<=bh<=8 and ratio>=3.5 and area>=13):
            continue
        fill=area/(bw*bh)
        if fill<.42:continue
        candidates.append({'box':[x,y,bw,bh], 'aspect_ratio':round(ratio,2),
                           'green_fill':round(fill,2)})
    return {'width':w,'height':h,'green_bar_candidates':candidates,
            'candidate_count':len(candidates),
            'meaning':'unverified horizontal green shapes; HP unknown'}


def assess(path):
    img=cv2.imread(str(path))
    if img is None:raise ValueError(f'unreadable: {path}')
    return {'file':str(path), **detect(img)}


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('frames',nargs='+')
    args=ap.parse_args()
    for path in args.frames:
        print(json.dumps(assess(Path(path)), ensure_ascii=False))


if __name__=='__main__': main()
