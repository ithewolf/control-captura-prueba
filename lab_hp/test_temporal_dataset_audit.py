import tempfile,unittest,json
from pathlib import Path
import cv2,numpy as np
from temporal_dataset_audit import audit
class Tests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
  d=self.root/"a";d.mkdir()
  for i in range(2):cv2.imwrite(str(d/f"f{i}.jpg"),np.full((40,60,3),50+i,dtype=np.uint8))
  self.manifest=d/"clips_manifest.json"
  self.clip={"clip_id":"clip00","source_video":"one","split":"UNASSIGNED","reviewed":False,
             "frames":[{"image":f"f{i}.jpg","time_s":float(i),"reviewed":False} for i in range(2)]}
  self.write()
 def tearDown(self):self.temp.cleanup()
 def write(self):self.manifest.write_text(json.dumps([self.clip]))
 def test_good(self):self.assertEqual(audit(self.root)["status"],"PASS")
 def test_unreviewed(self):self.assertEqual(audit(self.root)["human_reviewed_clips"],0)
 def test_missing(self):
  self.clip["frames"][0]["image"]="missing.jpg";self.write()
  self.assertEqual(audit(self.root)["status"],"FAIL")
 def test_invalid_timestamp(self):
  self.clip["frames"][1]["time_s"]=-1;self.write()
  self.assertEqual(audit(self.root)["status"],"FAIL")
 def test_false_review(self):
  self.clip["reviewed"]=True;self.write()
  self.assertEqual(audit(self.root)["status"],"FAIL")
 def test_unsafe_path(self):
  self.clip["frames"][0]["image"]="../../outside";self.write()
  self.assertEqual(audit(self.root)["status"],"FAIL")
if __name__=="__main__":unittest.main()
