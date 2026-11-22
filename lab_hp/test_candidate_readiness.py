import json,tempfile,unittest
from pathlib import Path
import cv2
import numpy as np
from candidate_readiness import audit_candidates
class TestReadiness(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.root=Path(self.t.name)
  d=self.root/"video1";(d/"images").mkdir(parents=True)
  cv2.imwrite(str(d/"images"/"frame.jpg"),np.full((80,120,3),128,dtype=np.uint8))
  (d/"candidate_manifest.json").write_text(json.dumps([{"image":"video1/images/frame.jpg","reviewed":False,"label_status":"PENDING_HUMAN_CVAT"}]))
 def tearDown(self):self.t.cleanup()
 def test_readable(self):
  r=audit_candidates(self.root)
  self.assertEqual((r["frames"],r["readable"]), (1,1))
 def test_blocked_without_labels(self):
  self.assertEqual(audit_candidates(self.root)["status"],"BLOCKED")
 def test_requires_independent_videos(self):
  self.assertIn("INSUFFICIENT_INDEPENDENT_VIDEOS_FOR_TRAIN_VAL_TEST",audit_candidates(self.root)["issues"])
 def test_rejects_fake_jpeg(self):
  (self.root/"video1/images/frame.jpg").write_bytes(b"not an image")
  self.assertEqual(audit_candidates(self.root)["invalid"],1)
 def test_missing(self):
  self.assertEqual(audit_candidates(self.root/"missing")["frames"],0)
if __name__=="__main__":unittest.main()
