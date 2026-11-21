"""Enforce honest evaluation on independent Wild Rift videos."""
import importlib.util
import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("check_yasuo_labels",ROOT/"tools"/"check_yasuo_labels.py")
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
def labeled(video,sec,**fields):
 return {"video":video,"second":sec,**fields}
class TestVideoLabelValidation(unittest.TestCase):
 def test_unknown_not_truth(self):
  with self.assertRaisesRegex(ValueError,"No verified human labels"):
   module.validate([labeled("yasuo_a",0,hud_visible=None)])
 def test_duplicate_is_invalid(self):
  with self.assertRaisesRegex(ValueError,"Duplicated"):
   module.validate([labeled("yasuo_a",0,hud_visible=True),
                    labeled("yasuo_a",0,hud_visible=False)])
 def test_same_video_leaks(self):
  with self.assertRaisesRegex(ValueError,"Data leakage"):
   module.check_split([labeled("yasuo_a",0,hud_visible=True)],
                      [labeled("yasuo_a",5,hud_visible=False)])
 def test_separate_videos(self):
  result=module.check_split([labeled("yasuo_a",0,hud_visible=True)],
                            [labeled("yasuo_b",0,hud_visible=False)])
  self.assertTrue(result["separate_videos"])
 def test_invalid_boolean_label_rejected(self):
  with self.assertRaisesRegex(ValueError,"bool or null"):
   module.validate([labeled("yasuo_a",0,hud_visible="yes")])
if __name__=="__main__":unittest.main()
