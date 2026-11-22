import tempfile,unittest
from pathlib import Path
from champion_dataset_gate import audit,labels_ok
class Tests(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.root=Path(self.t.name)
  for x in ("a","b"):
   (self.root/(x+".png")).write_bytes(b"fixture")
   (self.root/(x+".txt")).write_text("0 .5 .5 .2 .2")
 def tearDown(self):self.t.cleanup()
 def row(self,n,source="one",split="train",reviewed=True,negative=False):
  return dict(image=n+".png",labels=n+".txt",source_video=source,scene_id=n,split=split,reviewed=reviewed,negative=negative)
 def test_pass(self):self.assertEqual(audit([self.row("a")],self.root)["status"],"PASS")
 def test_cross_video_pass(self):self.assertEqual(audit([self.row("a"),self.row("b","two","test")],self.root)["status"],"PASS")
 def test_leakage(self):self.assertEqual(audit([self.row("a"),self.row("b","one","test")],self.root)["status"],"FAIL")
 def test_unreviewed(self):
  with self.assertRaises(ValueError):audit([self.row("a",reviewed=False)],self.root)
 def test_negative_ok(self):
  (self.root/"a.txt").write_text("")
  self.assertEqual(audit([self.row("a",negative=True)],self.root)["status"],"PASS")
 def test_negative_bad(self):
  with self.assertRaises(ValueError):audit([self.row("a",negative=True)],self.root)
 def test_invalid_class(self):
  with self.assertRaises(ValueError):labels_ok("8 .5 .5 .2 .2")
 def test_bad_box(self):
  with self.assertRaises(ValueError):labels_ok("0 .95 .5 .2 .2")
 def test_nan(self):
  with self.assertRaises(ValueError):labels_ok("0 nan .5 .2 .2")
 def test_dup(self):
  with self.assertRaises(ValueError):audit([self.row("a"),self.row("a")],self.root)
 def test_missing(self):
  (self.root/"a.txt").unlink()
  with self.assertRaises(ValueError):audit([self.row("a")],self.root)
 def test_traversal(self):
  x=self.row("a");x["image"]="../x.png"
  with self.assertRaises(ValueError):audit([x],self.root)
if __name__=="__main__":unittest.main()
