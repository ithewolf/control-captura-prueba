import unittest,tempfile,json
from pathlib import Path
import numpy as np,cv2
from champion_frame_sampler import sample,dhash
class Tests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
  self.video=self.root/"test.mp4";w=cv2.VideoWriter(str(self.video),cv2.VideoWriter_fourcc(*"mp4v"),10,(160,120))
  rng=np.random.default_rng(24)
  for _ in range(8):
   frame=rng.integers(20,230,(120,160,3),dtype=np.uint8)
   for __ in range(10):w.write(frame)
  w.release()
 def tearDown(self):self.temp.cleanup()
 def test_exports_unlabeled(self):
  result=sample(self.video,self.root/"out",interval_s=1,max_samples=4,min_hamming=5)
  self.assertEqual(result["candidates"],4)
  rows=json.loads(Path(result["manifest"]).read_text())
  self.assertTrue(all(x["reviewed"] is False and x["champion_identity"] is None for x in rows))
 def test_diversity(self):
  self.assertLessEqual(sample(self.video,self.root/"out",interval_s=1,max_samples=20)["candidates"],7)
 def test_missing_file(self):
  with self.assertRaises(FileNotFoundError):sample(self.root/"none",self.root/"out")
 def test_invalid_interval(self):
  with self.assertRaises(ValueError):sample(self.video,self.root/"out",interval_s=0)
 def test_hash_stable(self):
  a=np.zeros((120,160,3),dtype=np.uint8);self.assertEqual(dhash(a),dhash(a.copy()))
if __name__=="__main__":unittest.main()
