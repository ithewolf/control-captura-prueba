import tempfile, unittest, csv
from pathlib import Path
from tools.lab16_dataset_audit import audit
class DatasetTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.p=Path(self.tmp.name);(self.p/"images").mkdir();(self.p/"labels").mkdir()
        self.rows=[{"image":"a.jpg","video_id":"a","time_s":"10","source":"local",
                    "game_verified":"YES","scene_label":"GAMEPLAY","split":"development"},
                   {"image":"b.jpg","video_id":"b","time_s":"20","source":"local",
                    "game_verified":"YES","scene_label":"NO_GAMEPLAY","split":"holdout"}]
        for r in self.rows:
            (self.p/"images"/r["image"]).write_bytes(b"test")
            (self.p/"labels"/(Path(r["image"]).stem+".txt")).write_text("2 0.5 0.5 0.2 0.2\n")
        self.dump()
    def tearDown(self):self.tmp.cleanup()
    def dump(self):
        with (self.p/"manifest.csv").open("w",newline="") as f:
            w=csv.DictWriter(f,fieldnames=list(self.rows[0]));w.writeheader();w.writerows(self.rows)
    def report(self):return audit(self.p/"manifest.csv",self.p/"images",self.p/"labels")
    def test_valid_format_not_accuracy(self):
        r=self.report();self.assertEqual(r["status"],"READY_FOR_MODEL_EVALUATION")
        self.assertEqual(r["recognition_accuracy"],"NOT_MEASURED")
    def test_unverified_game(self):
        self.rows[0]["game_verified"]="NO";self.dump();self.assertEqual(self.report()["status"],"BLOCKED")
    def test_missing_label(self):
        (self.p/"labels"/"a.txt").unlink();self.assertEqual(self.report()["status"],"BLOCKED")
    def test_missing_image(self):
        (self.p/"images"/"b.jpg").unlink();self.assertEqual(self.report()["status"],"BLOCKED")
    def test_invalid_coordinates(self):
        (self.p/"labels"/"b.txt").write_text("2 1.3 0.5 0.2 0.2\n")
        self.assertEqual(self.report()["status"],"BLOCKED")
    def test_unreviewed_scene(self):
        self.rows[1]["scene_label"]="UNREVIEWED";self.dump();self.assertEqual(self.report()["status"],"BLOCKED")
    def test_leakage_same_video_split(self):
        # A split must not share a source video with holdout.
        self.rows[1]["video_id"]="a";self.dump()
        self.assertEqual(self.report()["status"],"BLOCKED")
if __name__=="__main__":unittest.main()
