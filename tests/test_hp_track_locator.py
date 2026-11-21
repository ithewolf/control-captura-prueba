import unittest,importlib.util,pathlib,sys
import cv2,numpy as np
p=pathlib.Path(__file__).resolve().parents[1]/"tools"/"hp_track_locator.py"
spec=importlib.util.spec_from_file_location("hp_track_locator",p)
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)

def make(fill=.5):
    image=np.full((432,768,3),(125,110,120),dtype=np.uint8)
    cv2.rectangle(image,(315,195),(435,201),(48,48,48),-1)
    if fill>0:cv2.rectangle(image,(315,195),(315+round(120*fill)-1,201),(0,220,35),-1)
    return image

class TrackTest(unittest.TestCase):
    def test_half(self):self.assertGreaterEqual(len(mod.locate(make(.5))["candidates"]),1)
    def test_quarter(self):self.assertGreaterEqual(len(mod.locate(make(.25))["candidates"]),1)
    def test_black(self):self.assertEqual(len(mod.locate(np.zeros((432,768,3),dtype=np.uint8))["candidates"]),0)
    def test_full_rejected(self):self.assertEqual(len(mod.locate(make(1))["candidates"]),0)
    def test_invalid(self):
        with self.assertRaises(ValueError):mod.locate(None)
    def test_no_health_claim(self):
        result=mod.locate(make(.5))
        self.assertIsNone(result["health_percent"])
        self.assertEqual(result["game_state"],"UNKNOWN")
if __name__=="__main__":unittest.main()
