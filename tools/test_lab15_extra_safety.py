"""Additional anti-overclaim regressions for Lab15 evaluation; all fixtures synthetic."""
import unittest
from tools.lab15_adversarial_acceptance import evaluate

def segment(time,event,model="detector-v1"):
    return {"video_id":"test_video","start_s":str(time),
            "end_s":str(time+3),"event":event,
            "model_id":model,"confidence":"0.95"}
def human(time,event):
    return {k:v for k,v in segment(time,event).items() if k not in ("model_id","confidence")}
class AdditionalSafetyTests(unittest.TestCase):
    def test_unreviewed_prediction_not_counted_as_precision(self):
        r=evaluate([human(0,"NO_GAMEPLAY"),human(20,"TEAM_FIGHT")],
                   [segment(0,"NO_GAMEPLAY"),segment(20,"TEAM_FIGHT"),segment(55,"GANK")],True,True)
        self.assertEqual(r["status"],"EVALUATED_WITH_FAILURES")
        self.assertEqual(r["predictions_outside_reviewed_intervals"],1)
    def test_negative_control_false_positive(self):
        r=evaluate([human(0,"NO_GAMEPLAY"),human(20,"TEAM_FIGHT")],
                   [segment(0,"TEAM_FIGHT"),segment(20,"TEAM_FIGHT")],True,True)
        self.assertEqual(r["false_positive_on_no_gameplay"],1)
    def test_no_gameplay_is_not_positive_gameplay_ground_truth(self):
        r=evaluate([human(0,"NO_GAMEPLAY")],[segment(0,"NO_GAMEPLAY")],True,True)
        self.assertEqual(r["status"],"BLOCKED")
    def test_cannot_mix_detectors(self):
        r=evaluate([human(0,"NO_GAMEPLAY"),human(20,"TEAM_FIGHT")],
                   [segment(0,"NO_GAMEPLAY","m1"),segment(20,"TEAM_FIGHT","m2")],True,True)
        self.assertIn("MULTIPLE_MODELS_WITHOUT_SEPARATE_SCORES",r["failures"])
    def test_unknown_game_or_view_blocks_even_perfect_predictions(self):
        r=evaluate([human(0,"NO_GAMEPLAY"),human(20,"TEAM_FIGHT")],
                   [segment(0,"NO_GAMEPLAY"),segment(20,"TEAM_FIGHT")],False,True)
        self.assertEqual(r["metrics"],"NOT_MEASURED")
    def test_self_declared_independence_insufficient(self):
        r=evaluate([human(0,"NO_GAMEPLAY"),human(20,"TEAM_FIGHT")],
                   [segment(0,"NO_GAMEPLAY"),segment(20,"TEAM_FIGHT")],True,False)
        self.assertEqual(r["status"],"BLOCKED")
if __name__=="__main__":unittest.main()
