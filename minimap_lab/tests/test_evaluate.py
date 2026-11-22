"""Synthetic positive and negative cases are NOT evidence of real game detection."""
import json
import tempfile
import unittest
from pathlib import Path
from PIL import Image
from minimap_lab.evaluate import evaluate

class TemplateEvaluationTests(unittest.TestCase):
    def test_synthetic_present_absent_and_locations(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            pixels=[(x*19+y*31+(x*y*13))%256 for y in range(5) for x in range(5)]
            template=Image.new("L",(5,5));template.putdata(pixels)
            template.save(root/"icon.png")
            background=Image.new("L",(30,20),30)
            present=background.copy()
            present.paste(template,(11,7))
            present.save(root/"present.png")
            background.save(root/"absent.png")
            manifest={"schema":"control-minimap-template-eval-v1","samples":[
                {"frame":"present.png","template":"icon.png","expected_present":True,"expected_x":11,"expected_y":7},
                {"frame":"absent.png","template":"icon.png","expected_present":False}
            ]}
            (root/"labels.json").write_text(json.dumps(manifest))
            report=evaluate(root/"labels.json")
            self.assertEqual(report["tallies"]["tp"],1)
            self.assertEqual(report["tallies"]["tn"],1)
            self.assertEqual(report["tallies"]["fp"],0)
            self.assertEqual(report["tallies"]["fn"],0)
            self.assertEqual(report["tallies"]["location_errors"],0)
            self.assertFalse(report["verified_visual_game_detection"])
    def test_rejects_path_escape(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            (root/"labels.json").write_text(json.dumps({"schema":"control-minimap-template-eval-v1","samples":[{"frame":"../escape.png","template":"icon.png","expected_present":False}]}))
            with self.assertRaises(ValueError):
                evaluate(root/"labels.json")
if __name__=="__main__":
    unittest.main()
