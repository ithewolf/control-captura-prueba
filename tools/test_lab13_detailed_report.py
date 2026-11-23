import unittest
from tools.lab13_detailed_report import make_report

def sample(**overrides):
    obj={"video_id":"yasuo_a","timestamp_s":40,"source":"MOTION_ONLY","detections":[]}
    obj.update(overrides)
    return obj

def det(obj,conf=.95,confirmed=True):
    return {"object_type":obj,"confidence":conf,"temporal_confirmed":confirmed}

class DetailedReportTests(unittest.TestCase):
    def test_motion_abstains(self):
        r=make_report(sample())
        self.assertFalse(r["tactics"]["can_recommend"])
        self.assertEqual(r["metrics"]["strategy_accuracy"],"NOT_MEASURED")
        self.assertEqual(r["tactics"]["plan_a"],"REASSESS")
    def test_motion_does_not_launder_fake_detections(self):
        r=make_report(sample(detections=[det("ENEMY_CHAMPION")],own_hp_percent=10,own_hp_verified=True))
        self.assertEqual(r["player_state"]["nearby_enemies_confirmed"],0)
        self.assertIsNone(r["player_state"]["own_hp_percent"])
    def test_first_frame_unconfirmed(self):
        r=make_report(sample(source="DETECTOR",detections=[det("ENEMY_CHAMPION",confirmed=False)]))
        self.assertFalse(r["tactics"]["can_recommend"])
    def test_low_confidence_rejected(self):
        r=make_report(sample(source="DETECTOR",detections=[det("ENEMY_CHAMPION",.7)]))
        self.assertFalse(r["tactics"]["can_recommend"])
    def test_verified_retreat_and_plan_c(self):
        r=make_report(sample(source="DETECTOR",detections=[det("ENEMY_CHAMPION")],
            own_hp_percent=20,own_hp_verified=True))
        self.assertTrue(r["tactics"]["can_recommend"])
        self.assertEqual(r["tactics"]["plan_a"],"DEFEND_AND_RETREAT")
        self.assertEqual(r["tactics"]["contingency_c"],"CONTINUE_RETREAT_IF_PLAYER_ATTACKS")
    def test_unverified_health_not_decision(self):
        r=make_report(sample(source="DETECTOR",detections=[det("ENEMY_CHAMPION")],own_hp_percent=1))
        self.assertEqual(r["tactics"]["plan_a"],"HOLD_AND_OBSERVE")
    def test_confirmed_enemy_without_hp(self):
        r=make_report(sample(source="DETECTOR",detections=[det("ENEMY_CHAMPION")]))
        self.assertEqual(r["tactics"]["plan_a"],"HOLD_AND_OBSERVE")
    def test_numeric_without_complete_count(self):
        r=make_report(sample(source="DETECTOR",detections=[det("ENEMY_CHAMPION"),det("ENEMY_CHAMPION")]))
        self.assertEqual(r["tactics"]["plan_a"],"HOLD_AND_OBSERVE")
    def test_numeric_with_complete_count(self):
        r=make_report(sample(source="DETECTOR",detections=[det("ENEMY_CHAMPION"),det("ENEMY_CHAMPION")],
                             ally_enemy_counts_complete=True))
        self.assertEqual(r["tactics"]["plan_a"],"DEFEND_AND_RETREAT")
    def test_event_candidate_not_recognition(self):
        r=make_report(sample(event_predictions=[{"event":"GANK","confidence":.97,"temporal_confirmed":True}]))
        self.assertEqual(r["events"]["strategic_event_recognition_status"],"NOT_MEASURED")
    def test_detector_event_unscored(self):
        r=make_report(sample(source="DETECTOR",event_predictions=[{"event":"TEAM_FIGHT","confidence":.95,
            "temporal_confirmed":True}]))
        self.assertEqual(r["events"]["strategic_event_recognition_status"],"PREDICTIONS_AVAILABLE_UNSCORED")
        self.assertEqual(r["metrics"]["strategy_accuracy"],"NOT_MEASURED")
    def test_bad_event_rejected(self):
        with self.assertRaises(ValueError):
            make_report(sample(event_predictions=[{"event":"MADE_UP","confidence":1}]))
    def test_hp_range_checked(self):
        with self.assertRaises(ValueError):
            make_report(sample(own_hp_percent=120))
    def test_different_video_kept(self):
        r=make_report(sample(video_id="fiora_2"))
        self.assertEqual(r["video_id"],"fiora_2")

if __name__=="__main__": unittest.main()
