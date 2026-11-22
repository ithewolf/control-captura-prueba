"""Estimate image change after camera translation; not champion or skill detection."""
import cv2
import numpy as np
from camera_compensation import camera_translation

def analyze_residual(previous,current,width=320):
    cam=camera_translation(previous,current)
    if cam.get("status")!="MEASURED":
        return {"status":"UNKNOWN","reason":"camera_unreliable","camera":cam,"ready_for_advice":False}
    if not isinstance(width,int) or not 128<=width<=960:raise ValueError("invalid analysis width")
    h,w=previous.shape[:2];scale=width/w;out_h=max(64,round(h*scale))
    prev=cv2.cvtColor(cv2.resize(previous,(width,out_h)),cv2.COLOR_BGR2GRAY)
    curr=cv2.cvtColor(cv2.resize(current,(width,out_h)),cv2.COLOR_BGR2GRAY)
    matrix=np.float32([[1,0,cam["dx_px"]*scale],[0,1,cam["dy_px"]*scale]])
    warped=cv2.warpAffine(prev,matrix,(width,out_h),flags=cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT)
    valid=np.full_like(prev,255,dtype=np.uint8)
    overlap=cv2.warpAffine(valid,matrix,(width,out_h),flags=cv2.INTER_NEAREST)>0
    y0,y1=int(out_h*.2),int(out_h*.75);x0,x1=int(width*.25),int(width*.75)
    overlap=overlap[y0:y1,x0:x1]
    if overlap.mean()<.8:return {"status":"UNKNOWN","reason":"poor_overlap","camera":cam,"ready_for_advice":False}
    a=warped[y0:y1,x0:x1];b=curr[y0:y1,x0:x1]
    delta=np.abs(a.astype(np.int16)-b.astype(np.int16))[overlap]
    if not delta.size:return {"status":"UNKNOWN","reason":"empty_roi","camera":cam,"ready_for_advice":False}
    return {"status":"MEASURED","camera":cam,
        "residual_median_luma":round(float(np.median(delta)),2),
        "residual_p90_luma":round(float(np.percentile(delta,90)),2),
        "changed_fraction":round(float(np.mean(delta>22)),3),
        "interpretation":"CAMERA_COMPENSATED_PIXEL_CHANGE_ONLY",
        "ready_for_advice":False,"champion_detected":False,"skill_detected":False}
