"""Generate unlabeled human CVAT review queue; no predictions are inferred."""
import csv,json
from pathlib import Path
import cv2
FIELDS=("source_id","time_s","image","width","height","readable","review_status","visibility","candidate_champions","bbox_count","reviewer_notes")
def build_queue(root,output):
    root=Path(root).resolve();out=Path(output)
    manifests=sorted(root.glob("*/candidate_manifest.json"))
    if not manifests:raise ValueError("no manifests")
    rows=[];seen=set()
    for manifest in manifests:
        entries=json.loads(manifest.read_text(encoding="utf-8"))
        if not isinstance(entries,list):raise ValueError("invalid manifest")
        for entry in entries:
            if not isinstance(entry,dict) or not isinstance(entry.get("image"),str):raise ValueError("invalid entry")
            file=(root/entry["image"]).resolve()
            if not file.is_relative_to(root) or file in seen:raise ValueError("unsafe or duplicate image")
            seen.add(file);frame=cv2.imread(str(file))
            valid=frame is not None and frame.ndim==3 and frame.size>0
            h,w=frame.shape[:2] if valid else (0,0)
            rows.append(dict(source_id=entry.get("source_video",""),time_s=entry.get("time_s",""),
                image=entry["image"],width=w,height=h,readable="yes" if valid else "no",
                review_status="PENDING",visibility="NOT_REVIEWED",candidate_champions="",
                bbox_count="",reviewer_notes=""))
    out.parent.mkdir(parents=True,exist_ok=True)
    with out.open("w",encoding="utf-8-sig",newline="") as f:
        writer=csv.DictWriter(f,fieldnames=FIELDS);writer.writeheader();writer.writerows(rows)
    return {"frames":len(rows),"readable":sum(r["readable"]=="yes" for r in rows),
            "unreviewed":len(rows),"sources":len(set(r["source_id"] for r in rows)),
            "model_trained":False}
