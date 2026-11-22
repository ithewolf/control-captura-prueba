import tempfile,unittest
from pathlib import Path
from preflight_voice import inspect_gradle
class Tests(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.path=Path(self.t.name)/"build.gradle"
 def tearDown(self):self.t.cleanup()
 def write(self,s):self.path.write_text(s)
 def test_protected(self):
  self.write("com.alphacephei:vosk-android:0.3.75@aar net.java.dev.jna:jna:5.18.1@aar vosk-model-small-es-0.42.zip")
  self.assertEqual(inspect_gradle(self.path)["status"],"PASS")
 def test_missing_jna(self):
  self.write("com.alphacephei:vosk-android:0.3.75@aar vosk-model-small-es-0.42.zip")
  self.assertIn("JNA_MISSING",inspect_gradle(self.path)["problems"])
 def test_training_tools_rejected(self):
  self.write("com.alphacephei:vosk-android:0.3.75@aar net.java.dev.jna:jna:5.18.1@aar vosk-model-small-es-0.42.zip ultralytics")
  self.assertEqual(inspect_gradle(self.path)["status"],"FAIL")
 def test_no_vosk(self):
  self.write("net.java.dev.jna:jna:5.18.1@aar vosk-model-small-es-0.42.zip")
  self.assertIn("VOSK_MISSING",inspect_gradle(self.path)["problems"])
if __name__=="__main__":unittest.main()
