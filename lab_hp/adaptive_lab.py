"""Stateful automatic performance mode selection for Lab 2 only.

No Android telemetry is hooked up. Advice is always disabled until validation.
"""
from auto_governor import AutoGovernor
from full_stack_harness import FiveStageLab

class AdaptiveLab:
    def __init__(self, camera, tracker=None, champion_detector=None,
                 action_model=None, mobile_onnx=None, user_limit="balanced", recovery_s=30):
        self.governor=AutoGovernor(user_limit=user_limit, recovery_s=recovery_s)
        self.pipeline=FiveStageLab(camera, tracker, champion_detector, action_model, mobile_onnx)

    def set_user_limit(self, limit):
        self.governor.set_user_limit(limit)

    def process(self, timestamp_s, previous_frame, frame, thermal,
                frame_drop_pct, battery_saver=False):
        decision=self.governor.update(timestamp_s, thermal, frame_drop_pct, battery_saver)
        if decision.mode=="pause":
            return {"profile":"pause", "reason":decision.reason, "mode_changed":decision.changed,
                    "stages":{}, "recommendation":None, "ready_for_live":False,
                    "all_five_executed":False}
        # Run only stages permitted by the governor; the static budget layer
        # is a second independent ceiling under adverse telemetry.
        report=self.pipeline.process(previous_frame, frame, thermal=thermal,
                                     frame_drop_pct=frame_drop_pct,
                                     requested=decision.mode)
        report["reason"]=decision.reason
        report["mode_changed"]=decision.changed
        report["ready_for_live"]=False
        report["recommendation"]=None
        return report
