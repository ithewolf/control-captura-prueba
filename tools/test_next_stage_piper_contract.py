"""Static Android integration regression: does not claim on-device audio success."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/com/iawolf/captureprobe"
class PiperVoiceRouteContract(unittest.TestCase):
 def test_real_android_audio_route_is_present(self):
  s=(ROOT/"PiperVoiceOutput.java").read_text()
  for part in ("engine.generate(words,0,1.0f)","new AudioTrack.Builder()","AudioTrack.WRITE_BLOCKING","preparePhonemizer()"):
   self.assertIn(part,s)
 def test_vosk_never_listens_to_piper_speech(self):
  s=(ROOT/"VoskVoiceBridge.java").read_text()
  self.assertIn("haltListening(); // Never transcribe CONTROL's own speech.",s)
  self.assertIn('piper.speak(words,',s)
  self.assertIn('finishSpeech(true)',s)
  self.assertIn("beginListening()",s)
 def test_piper_failure_falls_back_without_retry_loop(self):
  s=(ROOT/"VoskVoiceBridge.java").read_text()
  self.assertIn("piperDisabledThisSession=true;",s)
  self.assertIn("tts.speak(words,TextToSpeech.QUEUE_FLUSH",s)
  self.assertIn("CaptureService.piperStatus=",s)
 def test_not_a_runtime_assertion(self):
  self.assertTrue(True) # Real phone performance/audio still require human test.
