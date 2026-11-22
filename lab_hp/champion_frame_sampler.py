"""Extract unlabeled diverse video frames for human CVAT review; no champion guessing."""
import cv2,hashlib,json
from pathlib import Path
def dhash(frame):
    g=cv2.cvtColor(frame,cv2.COLOR_BGR2GRAY)
    a=cv2.resize(g,(9,8),interpolation=cv2.INTER_AREA)
    h=0
    for b in (a[:,1:]>a[:,:-1]).ravel():h=(h<<1)|int(b)
    return h
def sample(video,output,interval_s=60,max_samples=12,min_hamming=10):
    if not isinstance(interval_s,(int,float)) or interval_s<1 or not isinstance(max_samples,int) or not 1<=max_samples<=500 or not isinstance(min_hamming,int) or not 0<=min_hamming<=64:
        raise ValueError("invalid sampling settings")
    video=Path(video);output=Path(output)
    if not video.is_file():raise FileNotFoundError(video)
    cap=cv2.VideoCapture(str(video))
    try:
        fps=cap.get(cv2.CAP_PROP_FPS);frames=cap.get(cv2.CAP_PROP_FRAME_COUNT)
        if fps<=0 or frames<=0:raise ValueError("invalid video metadata")
        duration=frames/fps
        source=hashlib.sha256((video.name+f"|{frames}|{fps:.3f}").encode()).hexdigest()[:12]
        folder=output/source/"images";folder.mkdir(parents=True,exist_ok=True)
        rows=[];hashes=[];t=float(interval_s)
        while t<duration and len(rows)<max_samples:
            cap.set(cv2.CAP_PROP_POS_MSEC,t*1000);ok,frame=cap.read()
            if ok and frame is not None and frame.ndim==3:
                gray=cv2.cvtColor(frame,cv2.COLOR_BGR2GRAY)
                if float(gray.std())>=8:
                    h=dhash(frame)
                    if all((h^past).bit_count()>=min_hamming for past in hashes):
                        name=f"frame_{len(rows):03d}_{int(t*1000):09d}.jpg"
                        target=folder/name
                        if cv2.imwrite(str(target),frame,[cv2.IMWRITE_JPEG_QUALITY,90]):
                            hashes.append(h)
                            rows.append({"image":str(target.relative_to(output)),"source_video":source,
                                "source_filename":video.name,"time_s":round(t,3),"reviewed":False,
                                "label_status":"PENDING_HUMAN_CVAT","split":"UNASSIGNED",
                                "champion_identity":None})
            t+=interval_s
        manifest=output/source/"candidate_manifest.json"
        manifest.write_text(json.dumps(rows,indent=2,ensure_ascii=False),encoding="utf-8")
        return {"source_video":source,"candidates":len(rows),"manifest":str(manifest)}
    finally:cap.release()
