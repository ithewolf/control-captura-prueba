import unittest
from auto_governor import AutoGovernor
class Tests(unittest.TestCase):
 def test_starts_conservative(self):self.assertEqual(AutoGovernor().mode,'lite')
 def test_gradual_recovery(self):
  g=AutoGovernor('full',10)
  self.assertEqual(g.update(0,'normal',0).mode,'lite')
  self.assertEqual(g.update(10,'normal',0).mode,'balanced')
  self.assertEqual(g.update(20,'normal',0).mode,'full')
 def test_cap(self):
  g=AutoGovernor('lite',10)
  for t in range(0,80,10):self.assertEqual(g.update(t,'normal',0).mode,'lite')
 def test_heat(self):
  g=AutoGovernor('full',10);g.update(0,'normal',0);g.update(10,'normal',0)
  self.assertEqual(g.update(11,'severe',0).mode,'pause')
 def test_drop(self):self.assertEqual(AutoGovernor().update(0,'normal',17).mode,'pause')
 def test_moderate(self):self.assertEqual(AutoGovernor().update(0,'moderate',0).mode,'lite')
 def test_battery(self):self.assertEqual(AutoGovernor().update(0,'normal',0,True).mode,'lite')
 def test_nan(self):self.assertEqual(AutoGovernor().update(0,'normal',float('nan')).mode,'pause')
 def test_order(self):
  g=AutoGovernor();g.update(10,'normal',0);self.assertEqual(g.update(5,'normal',0).reason,'telemetry_invalid')
 def test_lower_cap(self):
  g=AutoGovernor('full',10);g.update(0,'normal',0);g.update(10,'normal',0)
  g.set_user_limit('lite');self.assertEqual(g.mode,'lite')
 def test_resume(self):
  g=AutoGovernor('full',10);g.update(0,'severe',0)
  self.assertEqual(g.update(1,'normal',0).mode,'pause')
  self.assertEqual(g.update(11,'normal',0).mode,'lite')
 def test_not_ready(self):self.assertFalse(AutoGovernor().update(0,'normal',0).ready_for_android)
if __name__=='__main__':unittest.main()
