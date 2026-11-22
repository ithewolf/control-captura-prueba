"""Profile-based camera translation estimation: offline experimental component."""
import cv2
import numpy as np

WIDTHS = {'lite':224, 'balanced':320, 'full':480}

def estimate(prev, curr, profile='balanced'):
    if profile not in WIDTHS:
        return {'status':'UNKNOWN','reason':'invalid_profile'}
    if not (isinstance(prev,np.ndarray) and isinstance(curr,np.ndarray)
            and prev.dtype==np.uint8 and curr.dtype==np.uint8
            and prev.ndim==3 and curr.ndim==3 and prev.shape==curr.shape
            and prev.shape[2]==3 and min(prev.shape[:2])>=64):
        return {'status':'UNKNOWN','reason':'invalid_frames'}
    h,w=prev.shape[:2]; factor=min(1., WIDTHS[profile]/max(w,h))
    ww=max(32,round(w*factor));hh=max(32,round(h*factor))
    a=cv2.cvtColor(cv2.resize(prev,(ww,hh)),cv2.COLOR_BGR2GRAY)
    b=cv2.cvtColor(cv2.resize(curr,(ww,hh)),cv2.COLOR_BGR2GRAY)
    mask=np.zeros((hh,ww),np.uint8)
    mask[int(hh*.2):int(hh*.75),int(ww*.2):int(ww*.8)]=255
    pts=cv2.goodFeaturesToTrack(a,160,.015,5,mask=mask)
    if pts is None or len(pts)<12:
        return {'status':'UNKNOWN','reason':'insufficient_features'}
    next_pts,valid,_=cv2.calcOpticalFlowPyrLK(a,b,pts,None,winSize=(15,15),maxLevel=2)
    if next_pts is None or valid is None:
        return {'status':'UNKNOWN','reason':'tracking_failed'}
    keep=valid.ravel()==1
    p=pts[keep].reshape(-1,2);q=next_pts[keep].reshape(-1,2)
    if len(p)<12:
        return {'status':'UNKNOWN','reason':'insufficient_tracks'}
    matrix,inliers=cv2.estimateAffinePartial2D(p,q,method=cv2.RANSAC,ransacReprojThreshold=2.5)
    if matrix is None or inliers is None or float(inliers.mean())<.6:
        return {'status':'UNKNOWN','reason':'inconsistent_camera'}
    return {'status':'MEASURED','dx_px':round(float(matrix[0,2])/factor,2),
            'dy_px':round(float(matrix[1,2])/factor,2),
            'inlier_fraction':round(float(inliers.mean()),3),
            'input_size':[ww,hh],'profile':profile,'ready_for_live':False}
