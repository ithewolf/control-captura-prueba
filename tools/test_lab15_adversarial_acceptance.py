import unittest
from tools.lab15_adversarial_acceptance import evaluate

def item(start,end,event,**kw):
    return {"video_id":"yasuo_a","start_s":str(start),"end_s":str(end),"event":event,**kw}
def lab(start,end,event): return item(start,end,event)
def pred(start,end,event):
    return item(start,end,event,model_id="actual-detector-v1",confidence="0.9")
def gt():return [lab(0,6,"NO_GAMEPLAY"),lab(38,44,"TEAM_FIGHT")]
def p():return [pred(0,6,"NO_GAMEPLAY"),pred(38,44,"TEAM_FIGHT")]
def run(labels=None,predictions=None,verified=True,independent=True):
    return evaluate(gt() if labels is None else labels,p() if predictions is None else predictions,verified,independent)
class AdversarialTests(unittest.TestCase):
    def test_valid_synthetic_oracle_fixture_mechanics_only(self):
        self.assertEqual(run()["status"],"EVALUATED")
    def test_profile_misread_as_fight(self):
        s=run(predictions=[pred(0,6,"TEAM_FIGHT"),pred(38,44,"TEAM_FIGHT")])
        self.assertEqual(s["false_positive_on_no_gameplay"],1)
        self.assertEqual(s["status"],"EVALUATED_WITH_FAILURES")
    def test_no_real_predictions(self):
        self.assertEqual(run(predictions=[])["status"],"BLOCKED")
    def test_no_labels(self):
        self.assertEqual(run(labels=[])["status"],"BLOCKED")
    def test_unknown_source(self):
        self.assertIn("GAME_AND_VIEW_NOT_VERIFIED",run(verified=False)["failures"])
    def test_labels_not_independent(self):
        self.assertEqual(run(independent=False)["status"],"BLOCKED")
    def test_missing_negative_control(self):
        self.assertEqual(run(labels=[lab(38,44,"TEAM_FIGHT")])["status"],"BLOCKED")
    def test_missing_positive_control(self):
        self.assertEqual(run(labels=[lab(0,6,"NO_GAMEPLAY")])["status"],"BLOCKED")
    def test_duplicate_predictions(self):
        self.assertEqual(run(predictions=p()+[pred(38,44,"TEAM_FIGHT")])["status"],"BLOCKED")
    def test_unknown_model(self):
        with self.assertRaises(ValueError):
            run(predictions=[item(0,6,"NO_GAMEPLAY",model_id="UNSPECIFIED",confidence="0.9")])
    def test_missing_confidence(self):
        with self.assertRaises(ValueError):run(predictions=[item(0,6,"NO_GAMEPLAY",model_id="x")])
    def test_unreviewed_pred(self):
        s=run(predictions=p()+[pred(100,104,"GANK")])
        self.assertEqual(s["predictions_outside_reviewed_intervals"],1)
    def test_wrong_event(self):
        self.assertEqual(run(predictions=[p()[0],pred(38,44,"GANK")])["metrics"]["fn"],1)
    def test_no_motion_inference_without_labels(self):
        self.assertEqual(evaluate()["status"],"BLOCKED")
if __name__=="__main__":unittest.main()
