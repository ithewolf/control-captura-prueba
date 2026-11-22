import unittest,tempfile,zipfile
from pathlib import Path
from apk_size_audit import inspect
class Tests(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory()
  self.path=Path(self.t.name)/"test.apk"
 def tearDown(self):self.t.cleanup()
 def make(self,entries):
  with zipfile.ZipFile(self.path,"w") as z:
   for k,v in entries.items():z.writestr(k,v)
 def test_pass(self):
  self.make({"classes.dex":"abc","assets/model-es/am/data":"a"})
  self.assertEqual(inspect(self.path,1)["status"],"PASS")
 def test_research_files_fail(self):
  self.make({"lab_hp/model.pt":"x"})
  self.assertEqual(inspect(self.path,1)["status"],"FAIL")
 def test_budget_fail(self):
  self.make({"classes.dex":"x"*5000})
  self.assertEqual(inspect(self.path,.00001)["status"],"FAIL")
 def test_invalid(self):
  with self.assertRaises(ValueError):inspect(self.path)
 def test_largest(self):
  self.make({"assets/a":"x"*500,"classes.dex":"x"})
  self.assertEqual(inspect(self.path,1)["largest_entries"][0]["path"],"assets/a")
if __name__=="__main__":unittest.main()
