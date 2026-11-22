import unittest
from adaptive_lab import AdaptiveLab

def stub(a,b):return {"status":"MEASURED"}
class Tests(unittest.TestCase):
 def test_starts_lite(self):
  lab=AdaptiveLab(stub,stub,stub,stub,stub,user_limit="full",recovery_s=10)
  r=lab.process(0,None,None,"normal",0)
  self.assertEqual(r["profile"],"lite")
  self.assertEqual(r["stages"]["action_recognition"]["status"],"SKIPPED_BY_BUDGET")
 def test_gradual_ramp(self):
  lab=AdaptiveLab(stub,stub,stub,stub,stub,user_limit="full",recovery_s=10)
  a=[lab.process(t,None,None,"normal",0)["profile"] for t in (0,10,20)]
  self.assertEqual(a,["lite","balanced","full"])
 def test_heat_pause(self):
  lab=AdaptiveLab(stub,stub,stub,stub,stub)
  r=lab.process(0,None,None,"severe",0)
  self.assertEqual(r["profile"],"pause")
  self.assertEqual(r["stages"],{})
 def test_user_limit(self):
  lab=AdaptiveLab(stub,stub,stub,stub,stub,user_limit="lite",recovery_s=10)
  self.assertEqual([lab.process(t,None,None,"normal",0)["profile"] for t in (0,10,20)],["lite"]*3)
 def test_emergency_no_hysteresis(self):
  lab=AdaptiveLab(stub,stub,stub,stub,stub,user_limit="full",recovery_s=10)
  for t in (0,10,20):lab.process(t,None,None,"normal",0)
  r=lab.process(21,None,None,"normal",16)
  self.assertEqual(r["profile"],"pause")
 def test_invalid_telemetry_abstains(self):
  lab=AdaptiveLab(stub,stub,stub,stub,stub)
  r=lab.process(0,None,None,"unknown",0)
  self.assertEqual(r["profile"],"pause")
 def test_no_advice(self):
  lab=AdaptiveLab(stub,stub,stub,stub,stub,user_limit="full",recovery_s=10)
  for t in (0,10,20):r=lab.process(t,None,None,"normal",0)
  self.assertIsNone(r["recommendation"])
  self.assertFalse(r["ready_for_live"])
if __name__=="__main__":unittest.main()
