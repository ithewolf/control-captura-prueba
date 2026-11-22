import unittest
from temporal_tactics import TacticalWindow, suggest_action

def obj(i="F", p=(.3,.4), team="ally", confidence=.96):
    return {"id":i,"center":p,"team":team,"confidence":confidence}

class Tests(unittest.TestCase):
    def test_one_frame(self):
        self.assertEqual(TacticalWindow().add(0,[obj()])["state"],"UNKNOWN")
    def test_motion(self):
        w=TacticalWindow(); w.add(0,[obj()])
        r=w.add(1,[obj(p=(.4,.4))])
        self.assertEqual(r["state"],"TRACKS_OBSERVED")
        self.assertAlmostEqual(r["velocity_normalized_per_second"]["F"][0],.1)
        self.assertIsNone(r["recommendation"])
    def test_identity_switch(self):
        w=TacticalWindow();w.add(0,[obj()])
        self.assertEqual(w.add(1,[obj(i="S")])["state"],"UNKNOWN")
    def test_team_switch(self):
        w=TacticalWindow();w.add(0,[obj()])
        self.assertEqual(w.add(1,[obj(team="enemy")])["state"],"UNKNOWN")
    def test_bad_confidence(self):
        w=TacticalWindow();w.add(0,[obj()])
        self.assertEqual(w.add(1,[obj(confidence=.4)])["state"],"UNKNOWN")
    def test_bad_coordinates(self):
        w=TacticalWindow();w.add(0,[obj()])
        self.assertEqual(w.add(1,[obj(p=(4,.4))])["state"],"UNKNOWN")
    def test_gap_reset(self):
        w=TacticalWindow(max_gap_s=1);w.add(0,[obj()])
        self.assertEqual(w.add(4,[obj()])["state"],"UNKNOWN")
    def test_reverse_timestamp(self):
        w=TacticalWindow();w.add(1,[obj()])
        self.assertEqual(w.add(.5,[obj()])["reason"],"non_monotonic_time")
    def test_invalid_input(self):
        self.assertEqual(TacticalWindow().add(0,"bad")["state"],"UNKNOWN")
    def test_no_unvalidated_advice(self):
        self.assertFalse(suggest_action({"state":"TRACKS_OBSERVED"})["ready_for_live_advice"])

if __name__=="__main__": unittest.main()
