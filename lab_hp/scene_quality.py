"""Conservative frame-screening for human labeling; not a model or game-state detector."""
import cv2
import numpy as np

def screen_frame(frame):
    if not isinstance(frame,np.ndarray) or frame.dtype!=np.uint8 or frame.ndim!=3 or frame.shape[2]!=3 or min(frame.shape[:2])<64:
        return {'status':'INVALID','reason':'invalid_frame'}
    h,w=frame.shape[:2]
    crop=frame[int(.22*h):int(.80*h),int(.22*w):int(.78*w)]
    hsv=cv2.cvtColor(crop,cv2.COLOR_BGR2HSV)
    dark=float(np.mean(hsv[:,:,2]<65))
    if dark>.80:
        label='REVIEW_OCCLUDED'
    elif dark>.35:
        label='REVIEW_AMBIGUOUS'
    else:
        label='CANDIDATE_VISIBLE'
    return {'status':label,'center_dark_fraction':round(dark,3),
            'reviewed':False,'champion_identity':None,'detection_accuracy':'NOT_MEASURED'}
