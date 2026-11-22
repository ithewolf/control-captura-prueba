import unittest
import numpy as np
import cv2
from camera_compensation import camera_translation
from resource_budget import choose_profile
class Tests(unittest.TestCase):
 def setUp(self):self.im=np.random.default_rng(27).integers(30,220,(240,400,3),dtype=np.uint8)
 def test_still(self):
  r=camera_translation(self.im,self.im.copy());self.assertEqual(r["status"],"MEASURED");self.assertLess(abs(r["dx_px"]),1)
 def test_shift(self):
  shift=cv2.warpAffine(self.im,np.float32([[1,0,5],[0,1,3]]),(400,240))
  r=camera_translation(self.im,shift);self.assertEqual(r["status"],"MEASURED");self.assertAlmostEqual(r["dx_px"],5,delta=1.1)
 def test_black(self):
  z=np.zeros_like(self.im);self.assertEqual(camera_translation(z,z)["status"],"UNKNOWN")
 def test_bad(self):self.assertEqual(camera_translation(None,self.im)["status"],"UNKNOWN")
 def test_normal(self):self.assertEqual(choose_profile("normal",0).name,"balanced")
 def test_heat(self):self.assertEqual(choose_profile("moderate",0).name,"lite")
 def test_severe(self):self.assertEqual(choose_profile("severe",0).name,"pause")
 def test_fps(self):self.assertEqual(choose_profile("normal",18).name,"pause")
 def test_unknown(self):self.assertEqual(choose_profile("UNKNOWN",0).name,"pause")
 def test_lite(self):self.assertEqual(choose_profile("normal",0,user_limit="lite").name,"lite")
if __name__=="__main__":unittest.main()
