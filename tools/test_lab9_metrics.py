"""Regression checks for honest Lab9 offline metrics. No private video required."""
import importlib.util
from pathlib import Path
import unittest

MODULE = Path(__file__).resolve().parent / "lab9_video_benchmark.py"
spec = importlib.util.spec_from_file_location("lab9_video_benchmark", MODULE)
bench = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bench)

SPLIT = {"yasuo_a": "development", "fiora_b": "held_out"}
def record(video_id="yasuo_a", time_s="60", champion_id="yasuo", team="ENEMY", **kwargs):
    return dict(video_id=video_id, time_s=time_s, champion_id=champion_id, team=team, **kwargs)

class MetricsTests(unittest.TestCase):
    def test_perfect_match(self):
        s=bench.evaluate([record()], [record(confidence=".90")], SPLIT)["splits"]["development"]
        self.assertEqual((s["true_positive"], s["false_positive"], s["false_negative"]), (1,0,0))
    def test_duplicate_prediction_is_false_positive(self):
        labels=[record()]
        predictions=[record(confidence=".95"), record(confidence=".85")]
        s=bench.evaluate(labels, predictions, SPLIT)["splits"]["development"]
        self.assertEqual((s["true_positive"], s["false_positive"], s["false_negative"]), (1,1,0))
    def test_wrong_identity_counts_fp_and_fn(self):
        s=bench.evaluate([record()], [record(champion_id="fiora",confidence=".90")], SPLIT)["splits"]["development"]
        self.assertEqual((s["true_positive"], s["false_positive"], s["false_negative"]), (0,1,1))
    def test_wrong_team_not_accepted(self):
        s=bench.evaluate([record()], [record(team="ALLY",confidence=".90")], SPLIT)["splits"]["development"]
        self.assertEqual((s["true_positive"], s["false_positive"], s["false_negative"]), (0,0,1))
    def test_split_isolation(self):
        s=bench.evaluate([record(video_id="fiora_b")],[record(video_id="fiora_b",confidence=".98")],SPLIT)["splits"]
        self.assertEqual(s["held_out"]["true_positive"],1)
        self.assertEqual(s["development"]["true_positive"],0)
    def test_unreviewed_timestamps_are_not_false_positives(self):
        labels=[record()]
        predictions=[record(confidence=".95"), record(time_s="200", confidence=".85")]
        scored=bench.evaluate(labels,predictions,SPLIT)["splits"]["development"]
        self.assertEqual((scored["true_positive"], scored["false_positive"]), (1,0))
        self.assertEqual(scored["unscored_predictions_outside_reviewed_frames"],1)
    def test_prediction_on_reviewed_ally_frame_counts_false_positive(self):
        labels=[record(team="ALLY")]
        predictions=[record(confidence=".9")]
        scored=bench.evaluate(labels,predictions,SPLIT)["splits"]["development"]
        self.assertEqual((scored["true_positive"],scored["false_positive"]),(0,1))
    def test_zero_reviewed_rows_are_not_measured(self):
        result=bench.evaluate([], [], SPLIT)
        self.assertEqual(result["status"], "NOT_MEASURED")
        self.assertEqual(result["splits"]["held_out"]["status"], "NOT_MEASURED")
    def test_partial_source_coverage_is_explicit(self):
        result=bench.evaluate([record()], [record(confidence=".9")], SPLIT)
        self.assertEqual(result["status"], "PARTIALLY_MEASURED")
        self.assertEqual(result["splits"]["held_out"]["status"], "NOT_MEASURED")
    def test_no_labels_no_metrics(self):
        self.assertEqual(bench.evaluate(None,[],SPLIT)["status"],"NOT_MEASURED")
    def test_unknown_source_rejected(self):
        with self.assertRaises(ValueError):
            bench.evaluate([record(video_id="bogus")],[],SPLIT)

if __name__=="__main__":
    unittest.main()
