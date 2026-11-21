"""Strict research locator for closed rectangular UI bar borders.

Only proposals; never computes game HP, allegiance or character identity.
A dark patch beyond color is NOT treated as evidence of bar width.
"""
import cv2
import numpy as np

def locate_candidates(bgr):
    if bgr is None or bgr.ndim != 3 or bgr.shape[2] != 3:
        raise ValueError('BGR image required')
    h,w=bgr.shape[:2]
    if h<60 or w<120:raise ValueError('frame too small')
    hsv=cv2.cvtColor(bgr,cv2.COLOR_BGR2HSV)
    green=cv2.inRange(hsv,(36,65,55),(87,255,255))
    red=cv2.bitwise_or(cv2.inRange(hsv,(0,85,65),(13,255,255)),
                       cv2.inRange(hsv,(170,85,65),(179,255,255)))
    mask=cv2.bitwise_or(green,red)
    edges=cv2.Canny(cv2.cvtColor(bgr,cv2.COLOR_BGR2GRAY),65,145)
    contours,_=cv2.findContours(edges,cv2.RETR_LIST,cv2.CHAIN_APPROX_SIMPLE)
    result=[]
    for contour in contours:
        x,y,bw,bh=cv2.boundingRect(contour)
        if not (20<=bw<=w*.3 and 4<=bh<=18 and bw>=bh*4.5):continue
        if x<w*.12 or x+bw>w*.96 or y<h*.05 or y+bh>h*.9:continue
        roi=mask[y+1:y+bh-1,x+1:x+bw-1]
        if not roi.size:continue
        active=(roi>0).mean(axis=0)>.27
        if active.sum()<max(5,int((bw-2)*.12)):continue
        idx=np.flatnonzero(active)
        if not len(idx) or (idx[-1]-idx[0]+1)>1.35*len(idx):continue
        rim=edges[max(0,y-1):min(h,y+bh+1),max(0,x-1):min(w,x+bw+1)]
        if rim.shape[0]<bh or rim.shape[1]<bw:continue
        if min((rim[:2,:]>0).mean(),(rim[-2:,:]>0).mean())<.35:continue
        if min((rim[:,:2]>0).mean(),(rim[:,-2:]>0).mean())<.22:continue
        result.append({'rect':[x,y,bw,bh],'fill_hint_pixels':int(active.sum()),
                       'status':'rectangular_bar_candidate','hp_percent':None})
    result.sort(key=lambda q:q['rect'][2]*q['rect'][3],reverse=True)
    filtered=[]
    for item in result:
        x,y,_,_=item['rect']
        if any(abs(x-a['rect'][0])<4 and abs(y-a['rect'][1])<4 for a in filtered):continue
        filtered.append(item)
    return filtered
