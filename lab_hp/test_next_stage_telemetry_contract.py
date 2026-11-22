"""Next-stage regression tests: no synthetic telemetry may enable vision."""
import unittest
from auto_governor import AutoGovernor

class LiveTelemetryContract(unittest.TestCase):
 def test_missing_telemetry_suspends_inference(self):
  g=AutoGovernor()
  for thermal,drop in ((None,0),("normal",None),("unknown",0)):
   self.assertEqual(g.update(1,thermal,drop).mode,"pause")
   self.assertFalse(g.update(2,thermal,drop).ready_for_android)
   g=AutoGovernor()
 def test_serious_thermal_pressure_pauses(self):
  for thermal in ("severe","critical"):
   d=AutoGovernor().update(1,thermal,0)
   self.assertEqual(d.mode,"pause")
   self.assertFalse(d.ready_for_android)
 def test_game_frame_drop_disables_analysis(self):
  self.assertEqual(AutoGovernor().update(1,"normal",15).mode,"pause")
 def test_cannot_resume_immediately(self):
  g=AutoGovernor(recovery_s=30)
  g.update(1,"severe",0)
  self.assertEqual(g.update(2,"normal",0).mode,"pause")
  self.assertEqual(g.update(33,"normal",0).mode,"lite")
 def test_no_claims_of_android_readiness(self):
  g=AutoGovernor()
  self.assertFalse(g.update(1,"normal",0).ready_for_android)
