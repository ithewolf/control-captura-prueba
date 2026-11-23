import unittest
from tools.lab14_review_windows import candidates
def sample():
    return {'status':'VISUAL_ACTIVITY_MEASURED','game_events_status':'NOT_MEASURED','samples':6,'duration_s':12,
            'records':[{'second':i*2,'mean_frame_change':v} for i,v in enumerate([None,1,2,30,20,3])]}
class ReviewWindowsTests(unittest.TestCase):
    def test_abstains(self):
        r=candidates(sample());self.assertEqual(r['strategy_recognition_status'],'NOT_MEASURED')
        self.assertTrue(all(x['game_event']=='UNLABELED' for x in r['review_windows']))
    def test_not_detector(self):
        r=sample();r['game_events_status']='MEASURED'
        with self.assertRaises(ValueError):candidates(r)
    def test_negative(self):
        r=sample();r['records'][1]['mean_frame_change']=-10
        with self.assertRaises(ValueError):candidates(r)
    def test_invalid_percentile(self):
        with self.assertRaises(ValueError):candidates(sample(),1.01)
    def test_duplicate_timestamp(self):
        r=sample();r['records'][2]['second']=2
        with self.assertRaises(ValueError):candidates(r)
    def test_empty(self):
        r=sample();r['records']=[]
        with self.assertRaises(ValueError):candidates(r)
    def test_window_bounds(self):
        self.assertTrue(all(x['end_s']<=12 for x in candidates(sample())['review_windows']))
if __name__=='__main__':unittest.main()
