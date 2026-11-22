"""Desktop CPU benchmark of OpenCV residual movement using human-supplied clips.

This DOES NOT benchmark recognition, ONNX, BoT-SORT, Android or Wild Rift FPS.
Usage:
  python lab_hp/benchmark_fiora_motion.py /path/to/CONTROL_Fiora_temporal_36 --out report.json
Requires actual user video frames to be extracted locally.
"""
import argparse,json,time,statistics,sys
from pathlib import Path
import cv2
from camera_compensation import camera_translation
def residual(a,b,width=320):
    cam=camera_translation(a,b)
    if cam.get("status")!="MEASURED":return {"status":"UNKNOWN"}
    h,w=a.shape[:2];scale=width/w;out_h=max(64,round(h*scale))
    prev=cv2.cvtColor(cv2.resize(a,(width,out_h)),cv2.COLOR_BGR2GRAY)
    curr=cv2.cvtColor(cv2.resize(b,(width,out_h)),cv2.COLOR_BGR2GRAY)
    M=cv2.getRotationMatrix2D((0,0),0,1)
    M[0,2]=cam["dx_px"]*scale;M[1,2]=cam["dy_px"]*scale
    shifted=cv2.warpAffine(prev,M,(width,out_h),flags=cv2.INTER_LINEAR)
    valid=cv2.warpAffine(prev*0+255,M,(width,out_h),flags=cv2.INTER_NEAREST)>0
    y0,y1=int(out_h*.2),int(out_h*.75);x0,x1=int(width*.25),int(width*.75)
    area=valid[y0:y1,x0:x1]
    if area.mean()<.8:return {"status":"UNKNOWN"}
    difference=cv2.absdiff(shifted[y0:y1,x0:x1],curr[y0:y1,x0:x1])[area]
    return {"status":"MEASURED","changed_fraction":round(float((difference>22).mean()),3)}
def measure(root,widths=(224,320,480),repeats=2):
    manifests=sorted(Path(root).glob("*/clips_manifest.json"))
    pairs=[]
    for path in manifests:
        for clip in json.loads(path.read_text(encoding="utf8")):
            ims=[cv2.imread(str(path.parent/f["image"])) for f in clip["frames"]]
            if any(im is None for im in ims):raise ValueError("unreadable frame")
            pairs.extend(zip(ims[:-1],ims[1:]))
    if not pairs:raise ValueError("no input clips")
    result={"pairs":len(pairs),"repeats":repeats,"runtime":"desktop_cpu","android_validated":False,"modes":{}}
    for width in widths:
        ms=[];status={}
        for _ in range(repeats):
            for a,b in pairs:
                t=time.perf_counter();r=residual(a,b,width);ms.append((time.perf_counter()-t)*1000)
                status[r["status"]]=status.get(r["status"],0)+1
        ms.sort()
        result["modes"][str(width)]={"runs":len(ms),"mean_ms":round(statistics.mean(ms),2),
                    "p95_ms":round(ms[int(.95*(len(ms)-1))],2),
                    "status_counts":status}
    return result
if __name__=="__main__":
    p=argparse.ArgumentParser();p.add_argument("root");p.add_argument("--out",default="benchmark.json");args=p.parse_args()
    r=measure(args.root);Path(args.out).write_text(json.dumps(r,indent=2));print(json.dumps(r,indent=2))
