"""Conservative temporal tracking observations. Not champion or skill recognition."""
import math
from collections import deque

def valid_number(x):
    return isinstance(x, (int, float)) and not isinstance(x, bool) and math.isfinite(x)

class TacticalWindow:
    def __init__(self, history=8, max_gap_s=2.0):
        if not isinstance(history, int) or history < 2 or not valid_number(max_gap_s) or max_gap_s <= 0:
            raise ValueError("invalid window")
        self.frames = deque(maxlen=history)
        self.max_gap_s = max_gap_s

    def add(self, timestamp_s, objects):
        if not valid_number(timestamp_s) or timestamp_s < 0 or not isinstance(objects, list):
            return {"state":"UNKNOWN","reason":"invalid_input","recommendation":None}
        if self.frames and timestamp_s <= self.frames[-1][0]:
            return {"state":"UNKNOWN","reason":"non_monotonic_time","recommendation":None}
        if self.frames and timestamp_s - self.frames[-1][0] > self.max_gap_s:
            self.frames.clear()
        valid = {}
        seen = set()
        for obj in objects:
            if not isinstance(obj, dict):
                continue
            ident,center,conf,team = (obj.get(k) for k in ("id","center","confidence","team"))
            if (not isinstance(ident,(str,int)) or isinstance(ident,bool) or ident in seen
                or team not in ("ally","enemy") or not isinstance(center,(tuple,list))
                or len(center)!=2 or not all(valid_number(v) and 0<=v<=1 for v in center)
                or not valid_number(conf) or not 0<=conf<=1):
                continue
            seen.add(ident)
            if conf >= .8:
                valid[ident] = {"center":tuple(center),"team":team}
        self.frames.append((timestamp_s, valid))
        if len(self.frames)<2:
            return {"state":"UNKNOWN","reason":"insufficient_history","recommendation":None}
        t0,a = self.frames[-2]
        t1,b = self.frames[-1]
        if not a or not b:
            return {"state":"UNKNOWN","reason":"insufficient_valid_objects","recommendation":None}
        matched = [i for i in a.keys() & b.keys() if a[i]["team"]==b[i]["team"]]
        if not matched:
            return {"state":"UNKNOWN","reason":"no_stable_tracks","recommendation":None}
        dt=t1-t0
        velocities={str(i):[round((b[i]["center"][j]-a[i]["center"][j])/dt,4)
                            for j in (0,1)] for i in matched}
        return {"state":"TRACKS_OBSERVED","reason":"stable_tracks",
                "velocity_normalized_per_second":velocities,
                "recommendation":None,"tactical_interpretation":"NOT_VALIDATED"}

def suggest_action(visual_state):
    """No tactical voice output until independently evaluated policy exists."""
    return {"recommendation":None,"reason":"tactical_policy_not_validated",
            "ready_for_live_advice":False}
