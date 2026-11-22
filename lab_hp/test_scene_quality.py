import unittest
import numpy as np
from scene_quality import screen_frame
class Tests(unittest.TestCase):
 def test_clear(self):self.assertEqual(screen_frame(np.full((120,200,3),180,dtype=np.uint8))['status'],'CANDIDATE_VISIBLE')
 def test_dark(self):self.assertEqual(screen_frame(np.zeros((120,200,3),dtype=np.uint8))['status'],'REVIEW_OCCLUDED')
 def test_invalid(self):self.assertEqual(screen_frame(None)['status'],'INVALID')
 def test_no_guess(self):self.assertIsNone(screen_frame(np.full((120,200,3),180,dtype=np.uint8))['champion_identity'])
 def test_no_accuracy_claim(self):self.assertEqual(screen_frame(np.full((120,200,3),180,dtype=np.uint8))['detection_accuracy'],'NOT_MEASURED')
if __name__=='__main__':unittest.main()
