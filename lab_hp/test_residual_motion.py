import unittest
import numpy as np,cv2
from residual_motion import analyze_residual
class Tests(unittest.TestCase):
 def setUp(self):self.base=np.random.default_rng(123).integers(20,230,(240,400,3),dtype=np.uint8)
 def test_identical(self):
  r=analyze_residual(self.base,self.base.copy())
  self.assertEqual(r["status"],"MEASURED");self.assertEqual(r["residual_median_luma"],0)
 def test_camera_only(self):
  moved=cv2.warpAffine(self.base,np.float32([[1,0,8],[0,1,4]]),(400,240))
  r=analyze_residual(self.base,moved,width=400)
  self.assertEqual(r["status"],"MEASURED");self.assertLess(r["changed_fraction"],.05)
 def test_local_object_motion(self):
  a=self.base.copy();b=self.base.copy()
  cv2.rectangle(a,(140,100),(210,165),(255,0,0),-1)
  cv2.rectangle(b,(180,100),(250,165),(255,0,0),-1)
  r=analyze_residual(a,b)
  self.assertIn(r["status"],("MEASURED","UNKNOWN"))
  self.assertFalse(r["ready_for_advice"])
 def test_reject_black(self):
  z=np.zeros_like(self.base);self.assertEqual(analyze_residual(z,z)["status"],"UNKNOWN")
 def test_no_claims(self):
  r=analyze_residual(self.base,self.base.copy())
  self.assertFalse(r["skill_detected"]);self.assertFalse(r["ready_for_advice"])
if __name__=="__main__":unittest.main()
