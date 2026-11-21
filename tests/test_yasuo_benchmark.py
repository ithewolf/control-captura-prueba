"""Synthetic tests for the Yasuo HUD benchmark. Not real-world recognition accuracy."""
import importlib.util
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("evaluate_yasuo",ROOT/"tools"/"evaluate_yasuo.py")
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class BenchmarkTests(unittest.TestCase):
 def test_empty_predictions_do_not_appear_correct(self):
  labels=[{"video":"a","second":10,"hud_visible":True}]
  result=module.evaluate(labels,[])
  d=result["fields"]["hud_visible"]
  self.assertEqual(d["abstained"],1)
  self.assertEqual(d["coverage"],0)
  self.assertIsNone(d["accuracy_on_decided"])

 def test_mismatched_video_does_not_count(self):
  labels=[{"video":"a","second":10,"hud_visible":True}]
  guesses=[{"video":"b","second":10,"hud_visible":True}]
  result=module.evaluate(labels,guesses)
  self.assertEqual(result["matched_predictions"],0)
  self.assertEqual(result["fields"]["hud_visible"]["coverage"],0)

 def test_false_positives_count(self):
  labels=[{"video":"a","second":10,"hud_visible":False},
          {"video":"a","second":11,"hud_visible":True}]
  guesses=[{"video":"a","second":10,"hud_visible":True},
           {"video":"a","second":11,"hud_visible":True}]
  result=module.evaluate(labels,guesses)
  d=result["fields"]["hud_visible"]
  self.assertEqual(d["fp"],1)
  self.assertEqual(d["tp"],1)
  self.assertEqual(d["precision"],0.5)
  self.assertEqual(d["recall"],1.0)

 def test_unknown_ground_truth_is_not_scored(self):
  labels=[{"video":"a","second":10,"hud_visible":None}]
  guesses=[{"video":"a","second":10,"hud_visible":True}]
  result=module.evaluate(labels,guesses)
  self.assertEqual(result["fields"]["hud_visible"]["labeled"],0)

if __name__=="__main__":unittest.main()
