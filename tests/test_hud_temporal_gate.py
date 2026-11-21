"""Unit tests for experimental temporal HUD confirmation (no CV dependencies)."""
import importlib.util
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "hud_temporal_gate.py"
spec = importlib.util.spec_from_file_location("hud_temporal_gate", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def item(time, yes):
    return {"timestamp_ms": time, "lower_right_circles": 13 if yes else 2,
            "bottom_right_core_circles": 6 if yes else 0,
            "player_controls_candidate": yes}


class GateTests(unittest.TestCase):
    def test_requires_three_consecutive(self):
        gate = module.HudTemporalGate()
        self.assertEqual(gate.update(item(1000, True))["status"], "unknown")
        self.assertEqual(gate.update(item(1500, True))["status"], "unknown")
        self.assertEqual(gate.update(item(2000, True))["status"], "player_hud_candidate")

    def test_single_false_positive_does_not_trigger(self):
        gate = module.HudTemporalGate()
        for idx, positive in enumerate((False, False, True, False, False, False)):
            outcome = gate.update(item(1000 + idx * 400, positive))
        self.assertEqual(outcome["status"], "no_player_hud_evidence")

    def test_long_gap_resets(self):
        gate = module.HudTemporalGate()
        gate.update(item(1000, True))
        gate.update(item(1500, True))
        self.assertEqual(gate.update(item(5000, True))["status"], "unknown")
        self.assertEqual(gate.update(item(5500, True))["status"], "unknown")
        self.assertEqual(gate.update(item(6000, True))["status"], "unknown")
        self.assertEqual(gate.update(item(6500, True))["status"], "player_hud_candidate")

    def test_old_or_duplicate_timestamp_resets(self):
        gate = module.HudTemporalGate()
        gate.update(item(1000, True))
        self.assertEqual(gate.update(item(1000, True))["status"], "unknown")
        self.assertEqual(gate.update(item(900, True))["status"], "unknown")

    def test_no_spectator_inference(self):
        gate = module.HudTemporalGate()
        for t in (1000, 1400, 1800):
            value = gate.update(item(t, False))
        self.assertEqual(value["status"], "no_player_hud_evidence")
        self.assertNotIn("spectator", value["status"])

    def test_mismatch_between_counts_and_candidate_rejected(self):
        gate = module.HudTemporalGate()
        bad = item(1000, True)
        bad["player_controls_candidate"] = False
        self.assertEqual(gate.update(bad)["status"], "unknown")

    def test_invalid_core_counts_rejected(self):
        gate = module.HudTemporalGate()
        bad = item(1000, True)
        bad["bottom_right_core_circles"] = 55
        self.assertEqual(gate.update(bad)["status"], "unknown")

    def test_state_switch_reconfirm(self):
        gate = module.HudTemporalGate()
        for t in (1000, 1400, 1800):
            gate.update(item(t, True))
        self.assertEqual(gate.update(item(2200, False))["status"], "unknown")
        self.assertEqual(gate.update(item(2600, False))["status"], "unknown")
        self.assertEqual(gate.update(item(3000, False))["status"], "no_player_hud_evidence")


if __name__ == "__main__":
    unittest.main()
