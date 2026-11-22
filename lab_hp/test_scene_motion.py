import unittest
import numpy as np
from scene_motion import estimate_motion

class MotionTests(unittest.TestCase):
    def setUp(self):
        rng=np.random.default_rng(42)
        self.frame=rng.integers(25,230,size=(360,640,3),dtype=np.uint8)
    def test_static(self):
        r=estimate_motion(self.frame,self.frame.copy())
        self.assertEqual(r["status"],"MEASURED")
        self.assertLess(r["p90_flow_px"],.1)
    def test_shift(self):
        r=estimate_motion(self.frame,np.roll(self.frame,8,axis=1))
        self.assertEqual(r["status"],"MEASURED")
        self.assertGreater(r["p90_flow_px"],1)
        self.assertFalse(r["ready_for_advice"])
    def test_black_abstains(self):
        z=np.zeros_like(self.frame)
        self.assertEqual(estimate_motion(z,z)["status"],"UNKNOWN")
    def test_invalid_abstains(self):
        self.assertEqual(estimate_motion(None,self.frame)["status"],"UNKNOWN")
    def test_shape_abstains(self):
        self.assertEqual(estimate_motion(self.frame,self.frame[:200])["status"],"UNKNOWN")

if __name__=="__main__":unittest.main()
