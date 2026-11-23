import unittest
from tools.lab11_strategy_eval import score

def row(video,start,end,event):
    return {"video_id":video,"start_s":str(start),"end_s":str(end),"event":event}

class Tests(unittest.TestCase):
    def test_missing_evidence(self):
        self.assertEqual(score()["status"],"NOT_MEASURED")
        self.assertEqual(score([],[])["status"],"NOT_MEASURED")
    def test_true_match(self):
        result=score([row("a",10,14,"TEAM_FIGHT")],[row("a",11,15,"TEAM_FIGHT")])
        self.assertEqual((result["true_positive"],result["false_positive"],result["false_negative"]),(1,0,0))
    def test_wrong_strategy(self):
        result=score([row("a",10,14,"RETREAT")],[row("a",10,14,"GANK")])
        self.assertEqual((result["true_positive"],result["false_positive"],result["false_negative"]),(0,1,1))
    def test_duplicate_pred(self):
        result=score([row("a",10,14,"GANK")],[row("a",10,14,"GANK"),row("a",10,14,"GANK")])
        self.assertEqual((result["true_positive"],result["false_positive"],result["false_negative"]),(1,1,0))
    def test_video_mismatch(self):
        result=score([row("a",10,14,"OBJECTIVE")],[row("b",10,14,"OBJECTIVE")])
        self.assertEqual(result["true_positive"],0)
    def test_window_mismatch(self):
        result=score([row("a",10,14,"TEAM_FIGHT")],[row("a",30,35,"TEAM_FIGHT")])
        self.assertEqual(result["true_positive"],0)
    def test_invalid_event(self):
        with self.assertRaises(ValueError):
            score([row("a",10,14,"NEXUS_DESTROYED")],[])
    def test_invalid_time(self):
        with self.assertRaises(ValueError):
            score([row("a",14,10,"RETREAT")],[])

if __name__=="__main__":
    unittest.main()
