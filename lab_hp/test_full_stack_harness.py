import unittest
from full_stack_harness import FiveStageLab
class Tests(unittest.TestCase):
 def test_missing_models(self):
  r=FiveStageLab(lambda a,b:{"status":"MEASURED"}).process(None,None)
  self.assertEqual(r["stages"]["tracking"]["status"],"UNAVAILABLE")
  self.assertFalse(r["all_five_executed"])
 def test_stubs_not_real(self):
  stub=lambda a,b:{"status":"MEASURED"}
  r=FiveStageLab(stub,stub,stub,stub,stub).process(None,None)
  self.assertTrue(r["all_five_executed"]);self.assertFalse(r["ready_for_live"])
 def test_exceptions(self):
  def bad(a,b):raise ValueError("test")
  self.assertEqual(FiveStageLab(bad).process(None,None)["stages"]["camera"]["status"],"UNKNOWN")
 def test_thermal_pause(self):
  r=FiveStageLab(lambda a,b:{"status":"MEASURED"}).process(None,None,thermal="severe")
  self.assertEqual(r["stages"],{})
if __name__=="__main__":unittest.main()
