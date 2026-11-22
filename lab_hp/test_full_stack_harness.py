import unittest
from full_stack_harness import FiveStageLab
class Tests(unittest.TestCase):
 def test_missing_models(self):
  r=FiveStageLab(lambda a,b:{"status":"MEASURED"}).process(None,None)
  self.assertEqual(r["stages"]["tracking"]["status"],"UNAVAILABLE")
  self.assertFalse(r["all_five_executed"])
 def test_full_stubs_not_real(self):
  stub=lambda a,b:{"status":"MEASURED"}
  r=FiveStageLab(stub,stub,stub,stub,stub).process(None,None,requested="full")
  self.assertTrue(r["all_five_executed"])
  self.assertFalse(r["ready_for_live"])
 def test_exceptions(self):
  def bad(a,b):raise ValueError("test")
  self.assertEqual(FiveStageLab(bad).process(None,None)["stages"]["camera"]["status"],"UNKNOWN")
 def test_thermal_pause(self):
  r=FiveStageLab(lambda a,b:{"status":"MEASURED"}).process(None,None,thermal="severe")
  self.assertEqual(r["stages"],{})
 def test_lite_skips_heavy_stages(self):
  called=[]
  def stub(a,b):called.append(1);return {"status":"MEASURED"}
  r=FiveStageLab(stub,stub,stub,stub,stub).process(None,None,requested="lite")
  self.assertEqual(len(called),3)
  self.assertEqual(r["stages"]["action_recognition"]["status"],"SKIPPED_BY_BUDGET")
  self.assertFalse(r["all_five_executed"])
 def test_balanced_skips_action_stage(self):
  called=[]
  def stub(a,b):called.append(1);return {"status":"MEASURED"}
  r=FiveStageLab(stub,stub,stub,stub,stub).process(None,None,requested="balanced")
  self.assertEqual(len(called),4)
  self.assertEqual(r["stages"]["action_recognition"]["status"],"SKIPPED_BY_BUDGET")
 def test_heat_lowers_workload(self):
  stub=lambda a,b:{"status":"MEASURED"}
  r=FiveStageLab(stub,stub,stub,stub,stub).process(None,None,thermal="moderate",requested="full")
  self.assertEqual(r["profile"],"lite")
if __name__=="__main__":unittest.main()
