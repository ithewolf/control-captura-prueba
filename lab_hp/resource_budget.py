"""Conservative profile selector. Needs real Android thermal and frame data to integrate."""
from dataclasses import dataclass
import math
@dataclass(frozen=True)
class Profile:
    name:str
    sample_fps:float
    width:int
    detector_every:int
    temporal:bool
PROFILES={
 "full":Profile("full",5,480,1,True),
 "balanced":Profile("balanced",2,320,2,True),
 "lite":Profile("lite",1,224,3,False),
 "pause":Profile("pause",0,0,0,False),
}
def choose_profile(thermal,game_frame_drop_pct,battery_saver=False,user_limit="balanced"):
    if thermal not in ("normal","moderate","severe","critical") or user_limit not in ("lite","balanced","full"):
        return PROFILES["pause"]
    if isinstance(game_frame_drop_pct,bool) or not isinstance(game_frame_drop_pct,(float,int)) or not math.isfinite(game_frame_drop_pct) or not 0<=game_frame_drop_pct<=100:
        return PROFILES["pause"]
    if thermal in ("severe","critical") or game_frame_drop_pct>=15:return PROFILES["pause"]
    if battery_saver or thermal=="moderate" or game_frame_drop_pct>=5:return PROFILES["lite"]
    if user_limit=="lite":return PROFILES["lite"]
    return PROFILES[user_limit]
