"""Lab2 next stage: no substitute metrics or adapters for measured mobile game."""
import unittest
from full_stack_harness import FiveStageLab

def fake_frame(*args):
 return {"status":"MEASURED","objects":0}

class LiveReadinessGate(unittest.TestCase):
 def test_one_mock_stage_cannot_approve(self):
  r=FiveStageLab(camera=fake_frame).process(None,None,requested="full")
  self.assertFalse(r["ready_for_live"])
  self.assertEqual(r["stages"]["tracking"]["status"],"UNAVAILABLE")
  self.assertEqual(r["stages"]["champion_detector"]["status"],"UNAVAILABLE")
 def test_five_mocks_still_not_real_world_proof(self):
  r=FiveStageLab(*([fake_frame]*5)).process(None,None,requested="full")
  self.assertTrue(r["all_five_executed"])
  self.assertFalse(r["ready_for_live"])
  self.assertIsNone(r["recommendation"])
 def test_safety_pause_disables_all(self):
  r=FiveStageLab(fake_frame).process(None,None,thermal="severe")
  self.assertEqual(r["profile"],"pause")
  self.assertEqual(r["stages"],{})
  self.assertFalse(r["ready_for_live"])
 def test_adaptor_failure_never_fabricates_action(self):
  def fail(*args):raise RuntimeError("unavailable")
  r=FiveStageLab(fail).process(None,None,requested="full")
  self.assertEqual(r["stages"]["camera"]["status"],"UNKNOWN")
  self.assertFalse(r["ready_for_live"])
