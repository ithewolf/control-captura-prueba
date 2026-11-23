#!/usr/bin/env python3
"""CONTROL Lab13: evidence-linked tactical reports, not an image classifier.
Input predictions must come from a separate detector. Missing proof -> abstain.
"""
import argparse
import json
from pathlib import Path

EVENTS = {"TEAM_FIGHT", "RETREAT", "GANK", "OBJECTIVE", "LANE_PUSH"}
OBJECTS = {"SELF", "ALLY_CHAMPION", "ENEMY_CHAMPION", "ALLY_MINION",
           "ENEMY_MINION", "TOWER", "DRAGON", "BARON"}
REQUIRED_KEYS = {"video_id", "timestamp_s", "source", "detections"}
MIN_CONFIDENCE = 0.80

def make_report(data):
    if not isinstance(data, dict) or not REQUIRED_KEYS.issubset(data):
        raise ValueError("video_id, timestamp_s, source and detections required")
    video_id = data["video_id"]
    if not isinstance(video_id, str) or not video_id.strip():
        raise ValueError("video_id required")
    timestamp = float(data["timestamp_s"])
    if not (0 <= timestamp <= 86400):
        raise ValueError("Invalid timestamp")
    source = data["source"]
    if source not in {"DETECTOR", "MOTION_ONLY"}:
        raise ValueError("source must identify actual detector or motion-only evidence")
    detections = data["detections"]
    if not isinstance(detections, list):
        raise ValueError("detections must be a list")
    accepted, rejected = [], []
    for item in detections:
        if not isinstance(item, dict) or item.get("object_type") not in OBJECTS:
            raise ValueError("Unsupported object detection")
        conf = float(item.get("confidence", -1))
        if not (0 <= conf <= 1):
            raise ValueError("Invalid detection confidence")
        candidate = {"object_type": item["object_type"],
                     "confidence": round(conf, 4),
                     "temporal_confirmed": item.get("temporal_confirmed") is True,
                     "model_id": item.get("model_id") or "UNSPECIFIED",
                     "bounding_box": item.get("bounding_box")}
        if source == "DETECTOR" and conf >= MIN_CONFIDENCE and candidate["temporal_confirmed"]:
            accepted.append(candidate)
        else:
            rejected.append({**candidate, "reason": ("MOTION_ONLY_NOT_DETECTION" if source == "MOTION_ONLY"
                             else "UNCONFIRMED_OR_LOW_CONFIDENCE")})
    event_candidates = data.get("event_predictions", [])
    if not isinstance(event_candidates, list):
        raise ValueError("event_predictions must be a list")
    accepted_events, rejected_events = [], []
    for e in event_candidates:
        if not isinstance(e, dict) or e.get("event") not in EVENTS:
            raise ValueError("Unknown event")
        conf = float(e.get("confidence", -1))
        if not 0 <= conf <= 1:
            raise ValueError("Invalid event confidence")
        normalized = {"event": e["event"], "confidence":round(conf,4),
                      "temporal_confirmed":e.get("temporal_confirmed") is True}
        if source == "DETECTOR" and conf >= MIN_CONFIDENCE and normalized["temporal_confirmed"]:
            accepted_events.append(normalized)
        else:
            rejected_events.append({**normalized,"reason":"EVENT_NOT_TEMPORALLY_CONFIRMED"})
    own_hp = data.get("own_hp_percent")
    hp_verified = data.get("own_hp_verified") is True
    if own_hp is not None:
        own_hp = float(own_hp)
        if not 0 <= own_hp <= 100:
            raise ValueError("Invalid own HP")
    own_hp = own_hp if hp_verified and source=="DETECTOR" else None
    enemies = sum(d["object_type"]=="ENEMY_CHAMPION" for d in accepted)
    allies = sum(d["object_type"]=="ALLY_CHAMPION" for d in accepted)
    risk = []
    plan_a=plan_b=plan_c="REASSESS"
    actionable = False
    if enemies and own_hp is not None and own_hp <= 30:
        risk.append("VERIFIED_ENEMY_AND_LOW_OWN_HP")
        plan_a, plan_b, plan_c = "DEFEND_AND_RETREAT", "SEEK_SAFE_POSITION", "CONTINUE_RETREAT_IF_PLAYER_ATTACKS"
        actionable = True
    elif enemies and data.get("ally_enemy_counts_complete") is True and enemies > allies + 1:
        risk.append("VERIFIED_NUMERICAL_DISADVANTAGE_IN_REVIEWED_AREA")
        plan_a, plan_b, plan_c = "DEFEND_AND_RETREAT", "SEEK_SAFE_POSITION", "REASSESS_WHEN_ALLIES_ARRIVE"
        actionable = True
    elif enemies:
        plan_a, plan_b, plan_c = "HOLD_AND_OBSERVE", "PREPARE_RETREAT", "REASSESS_ON_NEW_FRAME"
        actionable = True
    elif own_hp is not None and own_hp <= 25:
        plan_a, plan_b, plan_c = "RECOVER", "AVOID_EXPOSURE", "REASSESS_ON_NEW_FRAME"
        actionable = True
    reasons = ([f"CONFIRMED_{enemies}_ENEMY_CHAMPIONS"] if enemies else [])
    if own_hp is not None: reasons.append(f"CONFIRMED_OWN_HP_{own_hp:g}")
    if risk: reasons += risk
    if not actionable: reasons.append("INSUFFICIENT_CONFIRMED_EVIDENCE")
    report={
        "schema":1, "video_id":video_id.strip(),"timestamp_s":timestamp,
        "source":source, "visual_observations":{"accepted":accepted,"rejected":rejected},
        "events":{"confirmed_predictions":accepted_events,
                  "unconfirmed_predictions":rejected_events,
                  "strategic_event_recognition_status": ("PREDICTIONS_AVAILABLE_UNSCORED" if accepted_events
                                                        else "NOT_MEASURED")},
        "player_state":{"own_hp_percent":own_hp,"nearby_enemies_confirmed":enemies,
                        "nearby_allies_confirmed":allies,
                        "counts_are_complete":data.get("ally_enemy_counts_complete") is True},
        "tactics":{"plan_a":plan_a,"alternative_b":plan_b,"contingency_c":plan_c,
                   "can_recommend":actionable,"evidence_reasons":reasons,
                   "spoken_guidance": ("Retirate y buscá una posición segura." if plan_a=="DEFEND_AND_RETREAT"
                                      else "Hay un campeón enemigo confirmado. Evaluá la posición." if enemies
                                      else "No tengo información suficiente para recomendar una jugada.")},
        "metrics":{"recognition_precision":"NOT_MEASURED","recognition_recall":"NOT_MEASURED",
                   "strategy_accuracy":"NOT_MEASURED"},
        "limitations":["This does not inspect pixels or execute a game model.",
                       "Candidate event predictions are not scored without independent reviewed labels.",
                       "No unobserved map state, enemy skills, cooldowns or objectives may be inferred."],
    }
    return report

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--input",type=Path,required=True)
    parser.add_argument("--out",type=Path,required=True)
    args=parser.parse_args()
    report=make_report(json.loads(args.input.read_text(encoding="utf-8")))
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"report":str(args.out),"tactical_actionable":report["tactics"]["can_recommend"],
                      "strategy_recognition":report["events"]["strategic_event_recognition_status"]}))
if __name__=="__main__":main()
