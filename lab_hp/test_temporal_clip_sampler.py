import tempfile,unittest,json
from pathlib import Path
import cv2,numpy as np
from temporal_clip_sampler import sample_clips
class Tests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.root=Path(self.tmp.name)
  self.video=self.root/"synthetic.mp4"
  writer=cv2.VideoWriter(str(self.video),cv2.VideoWriter_fourcc(*"mp4v"),10,(160,120))
  rng=np.random.default_rng(42)
  for i in range(80):writer.write(rng.integers(10,200,(120,160,3),dtype=np.uint8))
  writer.release()
 def tearDown(self):self.tmp.cleanup()
 def test_export(self):
  r=sample_clips(self.video,self.root/"out",[2,5],frames_per_clip=4)
  self.assertEqual(r["frames"],8)
  rows=json.loads((self.root/"out/clips_manifest.json").read_text())
  self.assertTrue(all(f["abilities"] is None for c in rows for f in c["frames"]))
 def test_outside(self):
  with self.assertRaises(ValueError):sample_clips(self.video,self.root/"out",[.1])
 def test_missing(self):
  with self.assertRaises(FileNotFoundError):sample_clips(self.root/"missing",self.root/"out",[2])
 def test_invalid_count(self):
  with self.assertRaises(ValueError):sample_clips(self.video,self.root/"out",[2],frames_per_clip=1)
if __name__=="__main__":unittest.main()
