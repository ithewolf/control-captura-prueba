"""Lab-only optical-flow measurement: screen activity, not champion/skill recognition."""
import cv2
import numpy as np

def estimate_motion(previous_bgr, current_bgr, size=(320,180)):
    """Measure flow in the central gameplay viewport; never derive a tactical action."""
    if not all(isinstance(x,np.ndarray) and x.ndim==3 and x.shape[2]==3
               and x.dtype==np.uint8 and min(x.shape[:2])>=64
               for x in (previous_bgr,current_bgr)):
        return {"status":"UNKNOWN","reason":"invalid_frames"}
    if previous_bgr.shape != current_bgr.shape:
        return {"status":"UNKNOWN","reason":"shape_mismatch"}
    w,h=size
    a=cv2.cvtColor(cv2.resize(previous_bgr,(w,h)),cv2.COLOR_BGR2GRAY)
    b=cv2.cvtColor(cv2.resize(current_bgr,(w,h)),cv2.COLOR_BGR2GRAY)
    y0,y1=int(h*.15),int(h*.78)
    x0,x1=int(w*.28),int(w*.75)
    a,b=a[y0:y1,x0:x1],b[y0:y1,x0:x1]
    if float(np.std(a))<5 or float(np.std(b))<5:
        return {"status":"UNKNOWN","reason":"low_texture"}
    flow=cv2.calcOpticalFlowFarneback(a,b,None,.5,3,15,3,5,1.2,0)
    speed=np.linalg.norm(flow,axis=2)
    if not np.isfinite(speed).all():
        return {"status":"UNKNOWN","reason":"nonfinite_flow"}
    return {"status":"MEASURED","median_flow_px":round(float(np.median(speed)),3),
            "p90_flow_px":round(float(np.percentile(speed,90)),3),
            "moving_fraction":round(float(np.mean(speed>1)),3),
            "interpretation":"UNVERIFIED_SCREEN_MOTION_ONLY","ready_for_advice":False}
