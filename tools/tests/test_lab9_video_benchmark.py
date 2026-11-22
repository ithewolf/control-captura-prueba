import importlib.util
import json
import tempfile
import pathlib
import unittest

spec = importlib.util.spec_from_file_location("lab9",pathlib.Path(__file__).parents[1]/"lab9_video_benchmark.py")
lab9 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lab9)

class TestLab9(unittest.TestCase):
    def setUp(self):
        self.manifest={"videos":[{"id":str(x),"file":str(x)+".mp4",
                                   "split":"held_out" if x>=3 else "development"}
                                  for x in range(5)]}
    def test_manifest(self):
        self.assertEqual(len(lab9.validate_manifest(self.manifest)),5)
    def test_requires_ground_truth(self):
        self.assertEqual(lab9.evaluate(None,[],{})["status"],"NOT_MEASURED")
    def test_rejects_filename_as_label(self):
        labels=[{"video_id":"1","time_s":"30","champion_id":"warwick","team":"ENEMY"}]
        predicted=[{"video_id":"1","time_s":"30","champion_id":"yasuo","team":"ENEMY","confidence":".98"}]
        r=lab9.evaluate(labels,predicted,{"1":"development"})
        self.assertEqual(r["splits"]["development"]["true_positive"],0)
        self.assertEqual(r["splits"]["development"]["false_positive"],1)
    def test_confidence_gate(self):
        labels=[{"video_id":"1","time_s":"30","champion_id":"fiora","team":"ENEMY"}]
        predicted=[{"video_id":"1","time_s":"30","champion_id":"fiora","team":"ENEMY","confidence":".40"}]
        r=lab9.evaluate(labels,predicted,{"1":"development"})
        self.assertEqual(r["splits"]["development"]["true_positive"],0)
    def test_match(self):
        labels=[{"video_id":"4","time_s":"30","champion_id":"fiora","team":"ENEMY"}]
        predicted=[{"video_id":"4","time_s":"30.2","champion_id":"fiora","team":"ENEMY","confidence":".90"}]
        r=lab9.evaluate(labels,predicted,{"4":"held_out"})
        self.assertEqual(r["splits"]["held_out"]["true_positive"],1)
    def test_unknown_team_not_enemy(self):
        labels=[{"video_id":"4","time_s":"30","champion_id":"fiora","team":"ENEMY"}]
        predicted=[{"video_id":"4","time_s":"30","champion_id":"fiora","team":"UNKNOWN","confidence":".99"}]
        self.assertEqual(lab9.evaluate(labels,predicted,{"4":"held_out"})["splits"]["held_out"]["true_positive"],0)
if __name__=="__main__":unittest.main()
