"""Export short unlabeled video sequences to review champion motion/skills in CVAT."""
import cv2,json
from pathlib import Path

def sample_clips(video,dest,centers_s,frames_per_clip=6,step_s=.2,max_width=640):
    video=Path(video)
    if not video.is_file():raise FileNotFoundError(video)
    if not (isinstance(frames_per_clip,int) and 2<=frames_per_clip<=24 and
            .05<=step_s<=1 and 160<=max_width<=1920 and
            isinstance(centers_s,(list,tuple)) and 0<len(centers_s)<=30):
        raise ValueError("invalid sampling settings")
    cap=cv2.VideoCapture(str(video))
    try:
        fps=cap.get(cv2.CAP_PROP_FPS); n=cap.get(cv2.CAP_PROP_FRAME_COUNT)
        if fps<=0 or n<=0:raise ValueError("invalid video metadata")
        dest=Path(dest);dest.mkdir(parents=True,exist_ok=True);clips=[]
        for i,center in enumerate(centers_s):
            if not isinstance(center,(float,int)):raise ValueError("bad time")
            first=center-(frames_per_clip-1)*step_s/2
            if first<0 or first+(frames_per_clip-1)*step_s>=n/fps:raise ValueError("clip outside video")
            frames=[]
            for j in range(frames_per_clip):
                t=first+j*step_s;cap.set(cv2.CAP_PROP_POS_MSEC,t*1000)
                ok,im=cap.read()
                if not ok or im is None:raise ValueError("decode failed")
                h,w=im.shape[:2]
                if w>max_width:im=cv2.resize(im,(max_width,round(h*max_width/w)),interpolation=cv2.INTER_AREA)
                name=f"clip{i:02d}_frame{j:02d}.jpg"
                if not cv2.imwrite(str(dest/name),im):raise ValueError("write failed")
                frames.append({"image":name,"time_s":round(t,3),"reviewed":False,
                               "champions":None,"abilities":None,"camera_pan":None,"tactical_state":None})
            clips.append({"clip_id":f"clip{i:02d}","source_video":video.name,
                          "split":"UNASSIGNED","reviewed":False,"frames":frames})
        (dest/"clips_manifest.json").write_text(json.dumps(clips,ensure_ascii=False,indent=2),encoding="utf8")
        return {"clips":len(clips),"frames":sum(len(c["frames"]) for c in clips)}
    finally:cap.release()
