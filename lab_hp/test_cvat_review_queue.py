import tempfile,unittest,json,csv
from pathlib import Path
import cv2,numpy as np
from cvat_review_queue import build_queue
class Tests(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.root=Path(self.t.name);p=self.root/"video";(p/"images").mkdir(parents=True)
  cv2.imwrite(str(p/"images"/"one.jpg"),np.zeros((80,100,3),dtype=np.uint8))
  self.rows=[{"image":"video/images/one.jpg","source_video":"video","time_s":1}]
  self.manifest=p/"candidate_manifest.json";self.manifest.write_text(json.dumps(self.rows))
 def tearDown(self):self.t.cleanup()
 def run_queue(self):return build_queue(self.root,self.root/"queue.csv")
 def test_good(self):self.assertEqual(self.run_queue()["readable"],1)
 def test_unlabeled(self):
  self.run_queue()
  with (self.root/"queue.csv").open(encoding="utf-8-sig") as f:
   self.assertEqual(next(csv.DictReader(f))["candidate_champions"],"")
 def test_missing(self):
  self.manifest.unlink()
  with self.assertRaises(ValueError):self.run_queue()
 def test_duplicate(self):
  self.manifest.write_text(json.dumps(self.rows*2))
  with self.assertRaises(ValueError):self.run_queue()
 def test_traversal(self):
  self.rows[0]["image"]="../bad";self.manifest.write_text(json.dumps(self.rows))
  with self.assertRaises(ValueError):self.run_queue()
if __name__=="__main__":unittest.main()
