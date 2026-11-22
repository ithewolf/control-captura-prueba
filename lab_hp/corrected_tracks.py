"""Estimate relative movement after compensating screen-camera translation.

Research-only: detections and track IDs must come from a validated detector.
"""
import math

def corrected_tracks(previous, current, camera, elapsed_s):
    unknown = lambda reason: {"status":"UNKNOWN", "reason":reason, "tracks":[], "recommendation":None}
    if not isinstance(camera,dict) or camera.get("status")!="MEASURED":
        return unknown("unreliable_camera_motion")
    dx,dy=camera.get("dx_px"),camera.get("dy_px")
    if (not all(isinstance(x,(float,int)) and not isinstance(x,bool) and math.isfinite(x) for x in (dx,dy,elapsed_s))
        or elapsed_s<=0 or elapsed_s>2):
        return unknown("invalid_camera_or_time")
    if not isinstance(previous,list) or not isinstance(current,list):
        return unknown("invalid_tracks")
    def parse(items):
        result={}
        for item in items:
            if not isinstance(item,dict):continue
            ident=item.get("id");pt=item.get("center");confidence=item.get("confidence")
            if (not isinstance(ident,(str,int)) or isinstance(ident,bool) or ident in result or
                not isinstance(pt,(tuple,list)) or len(pt)!=2 or
                not all(isinstance(x,(int,float)) and not isinstance(x,bool) and math.isfinite(x) for x in pt) or
                not isinstance(confidence,(int,float)) or isinstance(confidence,bool) or
                not math.isfinite(confidence) or confidence<.8 or confidence>1):
                continue
            result[ident]=(float(pt[0]),float(pt[1]))
        return result
    before,after=parse(previous),parse(current)
    output=[]
    for ident in sorted(before.keys() & after.keys(),key=str):
        x0,y0=before[ident];x1,y1=after[ident]
        vx=(x1-x0-dx)/elapsed_s
        vy=(y1-y0-dy)/elapsed_s
        output.append({"id":ident,"relative_vx_px_s":round(vx,2),"relative_vy_px_s":round(vy,2)})
    if not output:return unknown("no_valid_identity_pairs")
    return {"status":"MEASURED", "tracks":output, "recommendation":None,
            "interpretation":"SCREEN_RELATIVE_MOVEMENT_ONLY","ready_for_live":False}
