#!/usr/bin/env python3
"""Offline minimap icon template evaluation. No champions/team are inferred.

An explicit manually labeled JSON dataset is required. Predictions are generic
icon matches; they are not validated Wild Rift enemy/ally classifications.
"""
import argparse
import json
import math
from pathlib import Path
from PIL import Image

MAX_SIDE = 320
MIN_VARIANCE = 12.0
THRESHOLD = 0.93

def image_gray(path):
    with Image.open(path) as im:
        im = im.convert("L")
        if not 3 <= im.width <= MAX_SIDE or not 3 <= im.height <= MAX_SIDE:
            raise ValueError("Image outside 3..320 bounds")
        return im.width, im.height, list(im.getdata())

def search(frame, pattern, threshold=THRESHOLD):
    fw, fh, pixels = frame
    tw, th, templ = pattern
    if tw > fw or th > fh or tw < 3 or th < 3:
        raise ValueError("Invalid template geometry")
    n = tw * th
    tm = sum(templ) / n
    tv = sum((v-tm)**2 for v in templ)
    if (tv/n)**0.5 < MIN_VARIANCE:
        return {"found": False, "x": None, "y": None, "score": 0.0}
    best = -2.0
    bx = by = None
    for y in range(fh-th+1):
        for x in range(fw-tw+1):
            values = [pixels[(y+j)*fw+x+i] for j in range(th) for i in range(tw)]
            vm = sum(values)/n
            pv = sum((v-vm)**2 for v in values)
            if pv == 0:
                continue
            dot = sum((a-tm)*(b-vm) for a,b in zip(templ,values))
            score = dot/math.sqrt(tv*pv)
            if score > best:
                best,bx,by = score,x,y
    return {"found": best >= threshold, "x": bx, "y": by, "score": round(max(-1.0,best),6)}

def evaluate(manifest_path):
    path = Path(manifest_path)
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("schema") != "control-minimap-template-eval-v1":
        raise ValueError("Unsupported dataset schema")
    samples = data.get("samples")
    if not isinstance(samples,list) or not samples or len(samples) > 1000:
        raise ValueError("Dataset must contain 1..1000 labeled cases")
    tallies = {"tp":0,"fp":0,"tn":0,"fn":0,"location_errors":0}
    rows = []
    for i,sample in enumerate(samples):
        if set(("frame","template","expected_present")) - set(sample):
            raise ValueError("Missing required sample fields")
        if type(sample["expected_present"]) is not bool:
            raise ValueError("expected_present must be boolean")
        frame_path = (path.parent / sample["frame"]).resolve()
        template_path = (path.parent / sample["template"]).resolve()
        # Never read outside dataset directory; no automatic downloads.
        root = path.parent.resolve()
        if not frame_path.is_relative_to(root) or not template_path.is_relative_to(root):
            raise ValueError("Paths must stay inside the dataset directory")
        predicted = search(image_gray(frame_path),image_gray(template_path))
        expected = sample["expected_present"]
        actual = predicted["found"]
        key = "tp" if expected and actual else "fn" if expected else "fp" if actual else "tn"
        tallies[key] += 1
        if expected and actual and "expected_x" in sample and "expected_y" in sample:
            if abs(sample["expected_x"]-predicted["x"])>2 or abs(sample["expected_y"]-predicted["y"])>2:
                tallies["location_errors"]+=1
        rows.append({"sample":i,"expected":expected,**predicted,"outcome":key})
    precision = tallies["tp"]/(tallies["tp"]+tallies["fp"]) if tallies["tp"]+tallies["fp"] else None
    recall = tallies["tp"]/(tallies["tp"]+tallies["fn"]) if tallies["tp"]+tallies["fn"] else None
    return {"dataset_cases":len(samples),"tallies":tallies,"precision":precision,"recall":recall,
            "verified_visual_game_detection":False,"cases":rows,
            "disclaimer":"Offline icon-template metrics only; not real champion/team recognition unless grounded in independently labeled game frames."}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("manifest")
    parser.add_argument("--report",required=True)
    args=parser.parse_args()
    result=evaluate(args.manifest)
    dest=Path(args.report)
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"cases":result["dataset_cases"],"tallies":result["tallies"],"precision":result["precision"],"recall":result["recall"]},ensure_ascii=False))

if __name__=="__main__":
    main()
