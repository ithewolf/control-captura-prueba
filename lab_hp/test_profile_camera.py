import unittest
import numpy as np
import cv2
from profile_camera import estimate
class TestProfileCamera(unittest.TestCase):
 def setUp(self):
  rng=np.random.default_rng(44)
  self.a=rng.integers(0,256,(360,640,3),dtype=np.uint8)
 def test_lite_resolution(self):
  r=estimate(self.a,self.a,'lite');self.assertEqual(r['status'],'MEASURED');self.assertEqual(r['input_size'][0],224)
 def test_full_resolution(self):
  r=estimate(self.a,self.a,'full');self.assertEqual(r['status'],'MEASURED');self.assertEqual(r['input_size'][0],480)
 def test_identity(self):
  r=estimate(self.a,self.a);self.assertLess(abs(r['dx_px']),1)
 def test_translation(self):
  b=cv2.warpAffine(self.a,np.float32([[1,0,9],[0,1,3]]),(640,360))
  r=estimate(self.a,b,'balanced');self.assertEqual(r['status'],'MEASURED');self.assertAlmostEqual(r['dx_px'],9,delta=2)
 def test_blank_abstains(self):
  b=np.zeros_like(self.a);self.assertEqual(estimate(b,b)['status'],'UNKNOWN')
 def test_bad_input(self):self.assertEqual(estimate(None,self.a)['status'],'UNKNOWN')
 def test_bad_profile(self):self.assertEqual(estimate(self.a,self.a,'turbo')['status'],'UNKNOWN')
 def test_not_android_ready(self):self.assertFalse(estimate(self.a,self.a)['ready_for_live'])
if __name__=='__main__':unittest.main()
