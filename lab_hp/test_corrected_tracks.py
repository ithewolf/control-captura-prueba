import unittest
from corrected_tracks import corrected_tracks as estimate

def obj(i,x,y,conf=.95):
    return {"id":i,"center":[x,y],"confidence":conf}
class TestCorrectedTracks(unittest.TestCase):
    def test_static_object_with_camera_pan(self):
        r=estimate([obj("a",40,40)],[obj("a",48,43)],{"status":"MEASURED","dx_px":8,"dy_px":3},1)
        self.assertEqual(r["status"],"MEASURED")
        self.assertEqual(r["tracks"][0]["relative_vx_px_s"],0)
    def test_independent_motion(self):
        r=estimate([obj("a",40,40)],[obj("a",58,43)],{"status":"MEASURED","dx_px":8,"dy_px":3},1)
        self.assertEqual(r["tracks"][0]["relative_vx_px_s"],10)
        self.assertFalse(r["ready_for_live"])
    def test_camera_unknown(self):
        self.assertEqual(estimate([],[],{"status":"UNKNOWN"},1)["status"],"UNKNOWN")
    def test_identity_switch(self):
        self.assertEqual(estimate([obj("a",40,40)],[obj("b",48,43)],{"status":"MEASURED","dx_px":8,"dy_px":3},1)["status"],"UNKNOWN")
    def test_low_confidence(self):
        self.assertEqual(estimate([obj("a",40,40)],[obj("a",48,43,.4)],{"status":"MEASURED","dx_px":8,"dy_px":3},1)["status"],"UNKNOWN")
    def test_invalid_time(self):
        self.assertEqual(estimate([],[],{"status":"MEASURED","dx_px":8,"dy_px":3},0)["status"],"UNKNOWN")
    def test_no_advice(self):
        self.assertIsNone(estimate([obj("a",40,40)],[obj("a",48,43)],{"status":"MEASURED","dx_px":8,"dy_px":3},1)["recommendation"])
if __name__=="__main__":unittest.main()
