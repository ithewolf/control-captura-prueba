"""Research-only full-track candidate locator. Not actual Wild Rift HP measurement."""
import cv2
import numpy as np

def locate(image, width=768):
    if image is None or image.ndim!=3 or image.shape[2]!=3 or min(image.shape[:2])<140:
        raise ValueError("invalid BGR frame")
    image=cv2.resize(image,(width,round(image.shape[0]*width/image.shape[1])),interpolation=cv2.INTER_AREA)
    h,w=image.shape[:2]
    hsv=cv2.cvtColor(image,cv2.COLOR_BGR2HSV)
    mask=cv2.inRange(hsv,(36,65,65),(87,255,255))
    mask[:int(h*.13),:]=0;mask[int(h*.8):,:]=0
    mask[:,:int(w*.2)]=0;mask[:,int(w*.85):]=0
    mask=cv2.morphologyEx(mask,cv2.MORPH_CLOSE,cv2.getStructuringElement(cv2.MORPH_RECT,(5,1)))
    n,lab,stats,centers=cv2.connectedComponentsWithStats(mask)
    out=[]
    for x,y,bw,bh,area in stats[1:]:
        x,y,bw,bh,area=map(int,(x,y,bw,bh,area))
        if not (16<=bw<=160 and 2<=bh<=12 and bw/bh>=4 and area>=26):continue
        if np.mean(mask[y:y+bh,x:x+bw]>0)<.52:continue
        extension=0;cy=y+bh//2
        for step in range(1,min(120,w-x-bw)+1):
            xx=x+bw+step-1
            patch=image[max(0,cy-1):min(h,cy+2),xx]
            if patch.size and patch.mean()<105:extension=step
            else:break
        if extension<5:continue
        out.append({"x":x,"y":y,"width_candidate":bw+extension,
                    "green_run":bw,"dark_extension":extension,
                    "status":"unverified_candidate"})
    return {"width":w,"height":h,"candidates":out,
            "game_state":"UNKNOWN","health_percent":None}
