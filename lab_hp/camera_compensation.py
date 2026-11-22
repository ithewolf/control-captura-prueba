"""OpenCV camera translation experiment; not champion recognition."""
import cv2
import numpy as np

def camera_translation(prev,curr):
    if not all(isinstance(a,np.ndarray) and a.dtype==np.uint8 and a.ndim==3 and a.shape[2]==3 for a in (prev,curr)) or prev.shape!=curr.shape or min(prev.shape[:2])<64:
        return {"status":"UNKNOWN","reason":"invalid_frames"}
    scale=min(1.,480./max(prev.shape[:2]))
    w,h=max(64,int(prev.shape[1]*scale)),max(64,int(prev.shape[0]*scale))
    p=cv2.cvtColor(cv2.resize(prev,(w,h)),cv2.COLOR_BGR2GRAY)
    q=cv2.cvtColor(cv2.resize(curr,(w,h)),cv2.COLOR_BGR2GRAY)
    mask=np.zeros((h,w),dtype=np.uint8)
    mask[int(.18*h):int(.72*h),int(.22*w):int(.77*w)]=255
    pts=cv2.goodFeaturesToTrack(p,350,.01,6,mask=mask)
    if pts is None or len(pts)<12:return {"status":"UNKNOWN","reason":"too_few_features"}
    tracked,status,_=cv2.calcOpticalFlowPyrLK(p,q,pts,None)
    if tracked is None or status is None:return {"status":"UNKNOWN","reason":"tracking_failed"}
    ok=status.ravel()==1
    a,b=pts[ok].reshape(-1,2),tracked[ok].reshape(-1,2)
    if len(a)<12:return {"status":"UNKNOWN","reason":"too_few_tracks"}
    matrix,inliers=cv2.estimateAffinePartial2D(a,b,method=cv2.RANSAC,ransacReprojThreshold=2.5)
    if matrix is None or inliers is None:return {"status":"UNKNOWN","reason":"ransac_failed"}
    f=float(inliers.mean())
    if f<.55:return {"status":"UNKNOWN","reason":"low_inlier_fraction","inlier_fraction":round(f,3)}
    return {"status":"MEASURED","dx_px":round(float(matrix[0,2])/scale,2),"dy_px":round(float(matrix[1,2])/scale,2),"inlier_fraction":round(f,3),"tracked_points":len(a),"camera_motion_only":True}
