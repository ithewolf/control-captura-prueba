"""No speaker audio used in CI. OpenVoice must fail safely before inference."""
from pathlib import Path
import subprocess,sys,tempfile,unittest
SCRIPT=Path(__file__).with_name("lab5_openvoice_v2.py")
class PrivateVoiceCloneGate(unittest.TestCase):
 def test_missing_reference_fails(self):
  with tempfile.TemporaryDirectory() as d:
   out=subprocess.run([sys.executable,str(SCRIPT),"--reference",str(Path(d)/"absent.mp3"),
   "--checkpoints",str(Path(d)),"--only-preflight"],capture_output=True,text=True)
   self.assertNotEqual(out.returncode,0)
   self.assertIn("Missing authorized voice reference",out.stderr+out.stdout)
 def test_missing_weights_fails_before_loading_ai(self):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/"private.wav";p.write_bytes(b"placeholder")
   out=subprocess.run([sys.executable,str(SCRIPT),"--reference",str(p),
   "--checkpoints",str(Path(d)),"--only-preflight"],capture_output=True,text=True)
   self.assertNotEqual(out.returncode,0)
   self.assertIn("Missing official OpenVoice V2 checkpoint",out.stdout+out.stderr)
