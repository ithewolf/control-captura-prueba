#!/usr/bin/env python3
"""Measure green fill of a MANUALLY CONFIRMED rectangular health bar.

Does not locate a bar, identify its owner, or claim health percentage from
an arbitrary crop. Measurements are candidate fill fractions, not true HP.
"""
from dataclasses import dataclass
import cv2
import numpy as np

@dataclass(frozen=True)
class FillObservation:
    fraction: float | None
    coverage: float
    evidence: str

def measure_green_fill(bgr: np.ndarray, rect: tuple[int,int,int,int]) -> FillObservation:
    """rect=(x,y,width,height) must describe the FULL bar's inner track."""
    x,y,w,h=rect
    if bgr is None or bgr.ndim!=3 or w<12 or h<2 or x<0 or y<0 or x+w>bgr.shape[1] or y+h>bgr.shape[0]:
        return FillObservation(None,0.0,'invalid_rectangle')
    crop=bgr[y:y+h,x:x+w]
    hsv=cv2.cvtColor(crop,cv2.COLOR_BGR2HSV)
    # Saturated greens only; secondary teal/blue resource bars excluded.
    mask=cv2.inRange(hsv,np.array([36,65,55]),np.array([87,255,255]))>0
    per_col=mask.mean(axis=0)
    active=per_col >= 0.35
    if not np.any(active):
        return FillObservation(None,0.0,'no_green_fill')
    first=int(np.argmax(active));last=int(np.flatnonzero(active)[-1])
    # A particle over terrain must not be interpreted as HP.
    continuity=float(active[first:last+1].mean())
    coverage=float(active.mean())
    if continuity < .75 or first>max(3,int(w*.15)):
        return FillObservation(None,coverage,'not_left_anchored_contiguous')
    return FillObservation(round((last+1)/w,3),coverage,'candidate_not_confirmed_hp')

class HpTrend:
    """Only compares readings tagged with independently established entity ID."""
    def __init__(self,max_gap_ms=1700,min_delta=.08):
        self.max_gap_ms=max_gap_ms;self.min_delta=min_delta;self.last={}
    def update(self,entity_id:str,ms:int,obs:FillObservation):
        if not entity_id or not isinstance(ms,int) or ms<0 or obs.fraction is None:
            return 'unknown'
        old=self.last.get(entity_id)
        self.last[entity_id]=(ms,obs.fraction)
        if old is None or ms<=old[0] or ms-old[0]>self.max_gap_ms:
            return 'unknown'
        delta=obs.fraction-old[1]
        if delta <= -self.min_delta:return 'fill_decreasing_candidate'
        if delta >= self.min_delta:return 'fill_increasing_candidate'
        return 'no_significant_change'

if __name__=='__main__':
    raise SystemExit('Research module: import measure_green_fill and HpTrend; no auto-detection yet.')
