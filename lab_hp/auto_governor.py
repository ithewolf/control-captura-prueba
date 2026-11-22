"""Conservative mode governor. Requires real Android telemetry integration."""
import math
from dataclasses import dataclass
LEVELS=("pause","lite","balanced","full")
@dataclass(frozen=True)
class Decision:
    mode:str
    reason:str
    changed:bool
    ready_for_android:bool=False
class AutoGovernor:
    def __init__(self,user_limit="balanced",recovery_s=30.):
        if user_limit not in LEVELS[1:] or not isinstance(recovery_s,(int,float)) or isinstance(recovery_s,bool) or not math.isfinite(recovery_s) or recovery_s<5:raise ValueError("invalid configuration")
        self.user_limit=user_limit
        self.recovery_s=float(recovery_s)
        self.mode="lite"
        self.last_timestamp=None
        self.healthy_since=None
    def set_user_limit(self,value):
        if value not in LEVELS[1:]:raise ValueError("invalid user limit")
        self.user_limit=value
        if LEVELS.index(self.mode)>LEVELS.index(value):self.mode=value
        self.healthy_since=None
    def update(self,timestamp_s,thermal,game_drop_pct,battery_saver=False):
        valid_time=isinstance(timestamp_s,(int,float)) and not isinstance(timestamp_s,bool) and math.isfinite(timestamp_s) and timestamp_s>=0
        valid_drop=isinstance(game_drop_pct,(int,float)) and not isinstance(game_drop_pct,bool) and math.isfinite(game_drop_pct) and 0<=game_drop_pct<=100
        valid_temp=thermal in ("normal","moderate","severe","critical")
        if not valid_time or not valid_drop or not valid_temp or not isinstance(battery_saver,bool) or (self.last_timestamp is not None and timestamp_s<=self.last_timestamp):
            prev=self.mode;self.mode="pause";self.healthy_since=None
            return Decision(self.mode,"telemetry_invalid",prev!=self.mode)
        self.last_timestamp=timestamp_s
        prev=self.mode
        if thermal in ("severe","critical") or game_drop_pct>=15:target,reason="pause","thermal_or_game_critical"
        elif battery_saver or thermal=="moderate" or game_drop_pct>=5:target,reason="lite","thermal_battery_or_game_pressure"
        else:target,reason=self.user_limit,"healthy"
        if LEVELS.index(target)<LEVELS.index(self.mode):
            self.mode=target;self.healthy_since=None
        elif LEVELS.index(target)>LEVELS.index(self.mode):
            if self.healthy_since is None:self.healthy_since=timestamp_s
            if timestamp_s-self.healthy_since>=self.recovery_s:
                self.mode=LEVELS[min(LEVELS.index(self.mode)+1,LEVELS.index(target))]
                self.healthy_since=timestamp_s;reason="gradual_recovery"
            else:reason="recovery_cooldown"
        else:self.healthy_since=None
        return Decision(self.mode,reason,self.mode!=prev)
