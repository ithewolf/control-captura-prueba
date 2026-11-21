"""Smoke test: cold-start trial resets session only and preserves trained model."""
import importlib.util
import pathlib
import tempfile
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("cold_start_trial",ROOT/"tools"/"cold_start_trial.py")
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)

class ColdStartTrialTests(unittest.TestCase):
 def test_reset_preserves_model_and_video(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d)
   model=root/"model.tflite";model.write_bytes(b"immutable-model")
   video=root/"yasuo.mp4";video.write_bytes(b"video-bytes")
   workspace=root/"sessions"
   first=mod.initialize(model,workspace,"run1",video,"replay")
   cache=workspace/"run1"/"session_cache"/"memory.tmp"
   cache.write_bytes(b"temporary memory")
   second=mod.initialize(model,workspace,"run1",video,"replay",reset=True)
   self.assertFalse(cache.exists())
   self.assertEqual(first["model_sha256"],second["model_sha256"])
   self.assertEqual(model.read_bytes(),b"immutable-model")
   self.assertEqual(video.read_bytes(),b"video-bytes")
   self.assertFalse(second["gameplay_understanding_verified"])
 def test_refuses_unknown_directory(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d)
   model=root/"model.bin";model.write_bytes(b"model")
   video=root/"yasuo.mp4";video.write_bytes(b"video")
   rogue=root/"sessions"/"run1";rogue.mkdir(parents=True)
   with self.assertRaisesRegex(ValueError,"session marker"):
    mod.initialize(model,root/"sessions","run1",video,"holdout",reset=True)
 def test_requires_new_run_or_explicit_reset(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d)
   model=root/"model.bin";model.write_bytes(b"model")
   video=root/"yasuo.mp4";video.write_bytes(b"video")
   mod.initialize(model,root/"sessions","run1",video,"holdout")
   with self.assertRaises(FileExistsError):
    mod.initialize(model,root/"sessions","run1",video,"holdout")
if __name__=="__main__":unittest.main()
