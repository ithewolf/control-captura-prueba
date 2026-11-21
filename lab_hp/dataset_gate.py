"""Validate CVAT YOLO labels and prevent train/val/test leakage. Offline only."""
import argparse
import json
from pathlib import Path
from collections import Counter, defaultdict
import cv2
import numpy as np

# Negative frames should have EMPTY label files, not 'not_a_health_bar' objects.
CLASSES=("health_bar_track","level_badge")
SPLITS={"train","val","test"}

def dhash(path):
    gray=cv2.imread(str(path),cv2.IMREAD_GRAYSCALE)
    if gray is None:raise ValueError("unreadable image: "+str(path))
    small=cv2.resize(gray,(9,8),interpolation=cv2.INTER_AREA)
    bits=(small[:,1:]>small[:,:-1]).ravel()
    out=0
    for b in bits:out=(out<<1)|int(b)
    return out

def labels_ok(path):
    counts=Counter()
    for n,line in enumerate(path.read_text(encoding="utf-8").splitlines(),1):
        if not line.strip():continue
        cols=line.split()
        if len(cols)!=5:raise ValueError(f"{path}:{n}: expected 5 fields")
        try:
            k=int(cols[0]);xc,yc,w,h=map(float,cols[1:])
        except ValueError as e:raise ValueError(f"{path}:{n}: invalid number") from e
        if k not in range(len(CLASSES)):raise ValueError(f"{path}:{n}: invalid class")
        if not all(np.isfinite(z) for z in (xc,yc,w,h)):
            raise ValueError(f"{path}:{n}: nonfinite box")
        if not (0<w<=1 and 0<h<=1 and 0<=xc<=1 and 0<=yc<=1 and
                xc-w/2>=-1e-6 and xc+w/2<=1+1e-6 and
                yc-h/2>=-1e-6 and yc+h/2<=1+1e-6):
            raise ValueError(f"{path}:{n}: invalid bounds")
        counts[CLASSES[k]]+=1
    return counts

def audit(manifest,root,max_hash_distance=3):
    if not isinstance(manifest,list) or not manifest:
        raise ValueError("manifest must be nonempty list")
    root=Path(root).resolve();videos=defaultdict(set);scenes=defaultdict(set)
    images=set();fingerprints=[];count=Counter();label_counts=Counter()
    issues=[]
    for i,row in enumerate(manifest):
        if not isinstance(row,dict):raise ValueError(f"row {i}: not an object")
        for key in ("image","source_video","scene_id","split"):
            if not isinstance(row.get(key),str) or not row[key].strip():
                raise ValueError(f"row {i}: missing {key}")
        # Official YOLO format accepts absent labels for negatives, but only
        # permit that when the manifest explicitly marks an empty-label image.
        if "labels" not in row or not isinstance(row["labels"],str) or not row["labels"].strip():
            raise ValueError(f"row {i}: labels path required for audit")
        if "negative" in row and type(row["negative"]) is not bool:
            raise ValueError(f"row {i}: negative must be bool")
        split=row["split"]
        if split not in SPLITS:raise ValueError(f"row {i}: invalid split")
        image=(root/row["image"]).resolve();lbl=(root/row["labels"]).resolve()
        if not image.is_relative_to(root) or not lbl.is_relative_to(root):
            raise ValueError(f"row {i}: path outside dataset")
        if image in images:raise ValueError(f"row {i}: repeated image")
        images.add(image)
        if not image.is_file():
            raise ValueError(f"row {i}: missing image")
        if lbl.is_file():
            counts=labels_ok(lbl)
            if row.get("negative",False) and sum(counts.values())>0:
                raise ValueError(f"row {i}: negative frame contains labels")
            if not row.get("negative",False) and sum(counts.values())==0:
                raise ValueError(f"row {i}: empty labels require explicit negative flag")
        elif row.get("negative",False):
            counts=Counter()  # YOLO permits missing txt for confirmed background.
        else:
            raise ValueError(f"row {i}: missing labels without negative flag")
        label_counts.update(counts);count[split]+=1
        videos[row["source_video"]].add(split)
        scenes[(row["source_video"],row["scene_id"])].add(split)
        fingerprints.append((split,dhash(image)))
    for video,parts in videos.items():
        if len(parts)>1:issues.append("SOURCE_LEAKAGE: "+video)
    for scene,parts in scenes.items():
        if len(parts)>1:issues.append("SCENE_LEAKAGE: "+str(scene))
    for i,(part,h) in enumerate(fingerprints):
        for part2,h2 in fingerprints[i+1:]:
            if part!=part2 and (h^h2).bit_count()<=max_hash_distance:
                issues.append("CROSS_SPLIT_NEAR_DUPLICATE")
    return {"status":"PASS" if not issues else "FAIL",
            "images":dict(count),"labels":dict(label_counts),"violations":issues,
            "note":"Data integrity is not detector accuracy"}

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--manifest",type=Path,required=True)
    ap.add_argument("--root",type=Path,required=True)
    a=ap.parse_args()
    result=audit(json.loads(a.manifest.read_text(encoding="utf-8")),a.root)
    print(json.dumps(result,indent=2,ensure_ascii=False))
    raise SystemExit(0 if result["status"]=="PASS" else 2)
if __name__=="__main__":main()
