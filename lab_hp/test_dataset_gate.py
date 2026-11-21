import unittest
from pathlib import Path
import tempfile
import json
import cv2
import numpy as np
from dataset_gate import audit,labels_ok

class DatasetGateTests(unittest.TestCase):
    def setUp(self):
        self.t=tempfile.TemporaryDirectory();self.root=Path(self.t.name)
        self.root.joinpath("a.png").parent.mkdir(exist_ok=True)
        im=np.full((50,80,3),(80,100,120),np.uint8)
        cv2.rectangle(im,(5,5),(25,14),(210,10,20),-1)
        cv2.imwrite(str(self.root/"a.png"),im)
        cv2.imwrite(str(self.root/"b.png"),np.flip(im,axis=1))
        (self.root/"a.txt").write_text("0 0.5 0.5 0.4 0.1\n")
        (self.root/"b.txt").write_text("")
    def tearDown(self):self.t.cleanup()
    def rows(self):
        return [{"image":"a.png","labels":"a.txt","source_video":"v1","scene_id":"s1","split":"train"},
                {"image":"b.png","labels":"b.txt","source_video":"v2","scene_id":"s2","split":"test"}]
    def test_valid(self):self.assertEqual(audit(self.rows(),self.root)["status"],"PASS")
    def test_negative_empty(self):self.assertEqual(dict(labels_ok(self.root/"b.txt")),{})
    def test_cross_video_leakage(self):
        r=self.rows();r[1]["source_video"]="v1"
        self.assertEqual(audit(r,self.root)["status"],"FAIL")
    def test_same_image(self):
        r=self.rows();r[1]["image"]="a.png"
        with self.assertRaises(ValueError):audit(r,self.root)
    def test_bad_class(self):
        (self.root/"a.txt").write_text("2 0.5 0.5 0.4 0.1\n")
        with self.assertRaises(ValueError):audit(self.rows(),self.root)
    def test_bad_geometry(self):
        (self.root/"a.txt").write_text("0 0.9 0.5 0.4 0.1\n")
        with self.assertRaises(ValueError):audit(self.rows(),self.root)
    def test_bad_nan(self):
        (self.root/"a.txt").write_text("0 nan 0.5 0.4 0.1\n")
        with self.assertRaises(ValueError):audit(self.rows(),self.root)
    def test_path_escape(self):
        r=self.rows();r[1]["image"]="../missing.png"
        with self.assertRaises(ValueError):audit(r,self.root)
    def test_missing_file(self):
        r=self.rows();r[1]["image"]="absent.png"
        with self.assertRaises(ValueError):audit(r,self.root)
    def test_malformed_fields(self):
        (self.root/"a.txt").write_text("0 0.5 0.5\n")
        with self.assertRaises(ValueError):audit(self.rows(),self.root)

if __name__=="__main__":unittest.main()
