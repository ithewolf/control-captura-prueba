#!/usr/bin/env python3
"""Conservative temporal filter for experimental Wild Rift HUD candidates.

Consumes JSON-lines created by hud_circle_probe.py with an additional timestamp_ms
field supplied by the caller. Produces one JSON line per observation.

This does NOT infer player state, enemies, skills, HP, or tactics. It never
fills missing observations using prior state and never trusts stale frames.
"""
import argparse
import json
import sys


class HudTemporalGate:
    """Confirm a candidate only after consecutive timely observations.

    Consecutive negative observations yield no_player_hud_evidence, not a
    definitive claim of spectator mode. Missing, irregular and old frames
    always yield unknown and reset the streak.
    """

    def __init__(self, required=3, max_gap_ms=1800, min_circles=10, min_core=4):
        if required < 2 or max_gap_ms <= 0:
            raise ValueError("invalid temporal gate configuration")
        self.required = required
        self.max_gap_ms = max_gap_ms
        self.min_circles = min_circles
        self.min_core = min_core
        self.last_ms = None
        self.streak = 0
        self.polarity = None

    def update(self, observation):
        timestamp = observation.get("timestamp_ms")
        counts = observation.get("lower_right_circles")
        core = observation.get("bottom_right_core_circles")
        valid = (type(timestamp) is int and timestamp >= 0 and
                 type(counts) is int and counts >= 0 and
                 type(core) is int and 0 <= core <= counts and
                 type(observation.get("player_controls_candidate")) is bool and
                 observation["player_controls_candidate"] ==
                 (counts >= self.min_circles and core >= self.min_core))
        if not valid or (self.last_ms is not None and
                         (timestamp <= self.last_ms or
                          timestamp - self.last_ms > self.max_gap_ms)):
            self.last_ms = timestamp if valid else None
            self.streak = 0
            self.polarity = None
            return {"status": "unknown", "stable": False,
                    "reason": "invalid_or_discontinuous_observation"}
        self.last_ms = timestamp
        positive = observation["player_controls_candidate"]
        if positive == self.polarity:
            self.streak += 1
        else:
            self.polarity = positive
            self.streak = 1
        if self.streak < self.required:
            return {"status": "unknown", "stable": False,
                    "reason": "awaiting_temporal_confirmation"}
        return {"status": ("player_hud_candidate" if positive else
                           "no_player_hud_evidence"),
                "stable": True, "reason": "consecutive_observations"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", nargs="?", default="-",
                        help="JSONL file or '-' for stdin")
    args = parser.parse_args()
    stream = sys.stdin if args.source == "-" else open(args.source, encoding="utf-8")
    try:
        gate = HudTemporalGate()
        for line in stream:
            if not line.strip():
                continue
            observation = json.loads(line)
            result = gate.update(observation)
            print(json.dumps({"timestamp_ms": observation.get("timestamp_ms"),
                              **result}, ensure_ascii=False))
    finally:
        if stream is not sys.stdin:
            stream.close()


if __name__ == "__main__":
    main()
