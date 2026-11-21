import unittest
import numpy as np
import cv2
from hp_pipeline import analyze, measure_fill

def img(fill=.5,color=(0,210,40)):
    a=np.full((120,240,3),(120,110,90),dtype=np.uint8)
    cv2.rectangle(a,(40,44),(180,54),(26,26,26),-1)
    cv2.rectangle(a,(40,44),(180,54),(200,200,200),1)
    if fill:cv2.rectangle(a,(41,45),(41+round(fill*138)-1,53),color,-1)
    return a

class LabTests(unittest.TestCase):
    def test_half_green(self):self.assertAlmostEqual(measure_fill(img(),(40,44,181,55))["fraction"],.5,delta=.03)
    def test_half_red(self):self.assertAlmostEqual(measure_fill(img(color=(20,20,210)),(40,44,181,55))["fraction"],.5,delta=.03)
    def test_full(self):self.assertAlmostEqual(measure_fill(img(1),(40,44,181,55))["fraction"],1,delta=.03)
    def test_empty_unknown(self):self.assertIsNone(measure_fill(img(0),(40,44,181,55))["fraction"])
    def test_invalid_box(self):self.assertIsNone(measure_fill(img(),(0,0,0,5))["fraction"])
    def test_confidence_gate(self):
        self.assertFalse(analyze(img(),[{"label":"health_bar_track","confidence":.3,"xyxy":[40,44,181,55]}])["observations"])
    def test_class_gate(self):
        self.assertFalse(analyze(img(),[{"label":"level_badge","confidence":.99,"xyxy":[40,44,181,55]}])["observations"])
    def test_no_claim(self):
        out=analyze(img(),[{"label":"health_bar_track","confidence":.99,"xyxy":[40,44,181,55]}])
        self.assertIsNone(out["observations"][0]["health_percent"])
        self.assertFalse(out["ready_for_android"])
    def test_black(self):self.assertIsNone(measure_fill(np.zeros((120,240,3),dtype=np.uint8),(40,44,181,55))["fraction"])

if __name__=="__main__":unittest.main()
