"""Unit tests: green horizontal candidates, no tactical meaning."""
import importlib.util
from pathlib import Path
import unittest
import numpy as np
import cv2

p=Path(__file__).resolve().parents[1]/'tools'/'health_bar_probe.py'
spec=importlib.util.spec_from_file_location('health_bar_probe',p)
probe=importlib.util.module_from_spec(spec);spec.loader.exec_module(probe)

def blank():return np.zeros((288,512,3),dtype=np.uint8)

class TestBarCandidates(unittest.TestCase):
 def test_black_frame_no_bar(self):
  self.assertEqual(probe.detect(blank())['candidate_count'],0)
 def test_horiz_green_center(self):
  im=blank();cv2.rectangle(im,(238,135),(282,138),(0,210,20),-1)
  self.assertGreaterEqual(probe.detect(im)['candidate_count'],1)
 def test_vertical_green_not_bar(self):
  im=blank();cv2.rectangle(im,(250,100),(254,160),(0,220,10),-1)
  self.assertEqual(probe.detect(im)['candidate_count'],0)
 def test_top_map_not_game_character(self):
  im=blank();cv2.rectangle(im,(245,5),(293,8),(0,220,10),-1)
  self.assertEqual(probe.detect(im)['candidate_count'],0)
 def test_red_bar_not_green_bar(self):
  im=blank();cv2.rectangle(im,(240,135),(280,138),(0,0,235),-1)
  self.assertEqual(probe.detect(im)['candidate_count'],0)
 def test_output_never_claims_health(self):
  out=probe.detect(blank());self.assertIn('HP unknown',out['meaning']);self.assertNotIn('health_percent',out)

if __name__=='__main__':unittest.main()
