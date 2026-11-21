"""Unit tests: full rectangle, no real health claims."""
import importlib.util
from pathlib import Path
import unittest
import cv2
import numpy as np
p=Path(__file__).resolve().parents[1]/'tools'/'hp_rectangle_probe.py'
spec=importlib.util.spec_from_file_location('hp_rectangle_probe',p)
probe=importlib.util.module_from_spec(spec)
import sys
sys.modules[spec.name]=probe
spec.loader.exec_module(probe)

def frame(fill=1,kind='green'):
    im=np.zeros((100,220,3),np.uint8)
    cv2.rectangle(im,(30,39),(169,47),(35,35,35),-1)
    color=(0,220,40) if kind=='green' else (225,180,0)
    if fill:cv2.rectangle(im,(30,39),(30+round(fill*140)-1,47),color,-1)
    return im

class HpTests(unittest.TestCase):
    def test_full(self):self.assertAlmostEqual(probe.measure_green_fill(frame(),(30,39,140,9)).fraction,1.0)
    def test_half(self):self.assertAlmostEqual(probe.measure_green_fill(frame(.5),(30,39,140,9)).fraction,.5)
    def test_30(self):self.assertAlmostEqual(probe.measure_green_fill(frame(.3),(30,39,140,9)).fraction,.3)
    def test_empty_is_unknown_not_zero_hp(self):self.assertIsNone(probe.measure_green_fill(frame(0),(30,39,140,9)).fraction)
    def test_blue_bar_rejected(self):self.assertIsNone(probe.measure_green_fill(frame(1,'blue'),(30,39,140,9)).fraction)
    def test_invalid_crop(self):self.assertEqual(probe.measure_green_fill(frame(),(-1,39,140,9)).evidence,'invalid_rectangle')
    def test_green_particle_right_rejected(self):
        im=frame(0);cv2.rectangle(im,(145,39),(162,47),(0,255,0),-1)
        self.assertIsNone(probe.measure_green_fill(im,(30,39,140,9)).fraction)
    def test_decreasing(self):
        t=probe.HpTrend()
        self.assertEqual(t.update('one',100,probe.measure_green_fill(frame(.9),(30,39,140,9))),'unknown')
        self.assertEqual(t.update('one',500,probe.measure_green_fill(frame(.5),(30,39,140,9))),'fill_decreasing_candidate')
    def test_new_identity_not_compared(self):
        t=probe.HpTrend();t.update('a',100,probe.measure_green_fill(frame(.9),(30,39,140,9)))
        self.assertEqual(t.update('b',500,probe.measure_green_fill(frame(.5),(30,39,140,9))),'unknown')
    def test_stale_not_compared(self):
        t=probe.HpTrend();t.update('a',100,probe.measure_green_fill(frame(.9),(30,39,140,9)))
        self.assertEqual(t.update('a',3000,probe.measure_green_fill(frame(.5),(30,39,140,9))),'unknown')
if __name__=='__main__':unittest.main()
