import unittest,importlib.util,pathlib
import cv2,numpy as np
p=pathlib.Path(__file__).resolve().parents[1]/'tools'/'hp_frame_edges.py'
spec=importlib.util.spec_from_file_location('hp_frame_edges',p)
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
def scene(fill=.5,bar='green',border=True,terrain=False):
 im=np.full((250,500,3),(66,86,98),np.uint8)
 color=(20,205,40) if bar=='green' else (18,30,210)
 if terrain:cv2.rectangle(im,(210,95),(280,98),color,-1)
 else:
  if border:cv2.rectangle(im,(195,92),(315,102),(220,220,220),1)
  cv2.rectangle(im,(196,93),(314,101),(25,25,25),-1)
  if fill:cv2.rectangle(im,(197,94),(197+round(fill*116)-1,100),color,-1)
 return im
class Tests(unittest.TestCase):
 def test_green_half(self):self.assertGreaterEqual(len(mod.locate_candidates(scene())),1)
 def test_red_half(self):self.assertGreaterEqual(len(mod.locate_candidates(scene(bar='red'))),1)
 def test_green_low(self):self.assertGreaterEqual(len(mod.locate_candidates(scene(fill=.2))),1)
 def test_full(self):self.assertGreaterEqual(len(mod.locate_candidates(scene(fill=1))),1)
 def test_terrain_without_frame(self):self.assertEqual(mod.locate_candidates(scene(terrain=True)),[])
 def test_no_fill_abstains(self):self.assertEqual(mod.locate_candidates(scene(fill=0)),[])
 def test_does_not_claim_hp(self):self.assertTrue(all(d['hp_percent'] is None for d in mod.locate_candidates(scene())))
 def test_invalid(self):
  with self.assertRaises(ValueError):mod.locate_candidates(None)
if __name__=='__main__':unittest.main()
