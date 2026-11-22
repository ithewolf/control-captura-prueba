import unittest,tempfile,zipfile
from pathlib import Path
from package_inventory import analyze

class PackageInventoryTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.path=Path(self.tmp.name)/"test.apk"
    def tearDown(self):self.tmp.cleanup()
    def apk(self,entries):
        with zipfile.ZipFile(self.path,"w",compression=zipfile.ZIP_DEFLATED) as z:
            for path,data in entries.items():z.writestr(path,data)
    def test_good_voice_inventory(self):
        self.apk({"classes.dex":b"a","assets/model-es/am/model":b"x","lib/arm64-v8a/libvosk.so":b"y","lib/armeabi-v7a/libvosk.so":b"z"})
        r=analyze(self.path);self.assertEqual(r["status"],"PASS")
        self.assertEqual(r["native_abis"],["arm64-v8a","armeabi-v7a"])
    def test_emulator_abi_rejected(self):
        self.apk({"assets/model-es/am/model":b"x","lib/arm64-v8a/libvosk.so":b"y","lib/armeabi-v7a/libvosk.so":b"z","lib/x86/libvosk.so":b"x"})
        self.assertIn("EMULATOR_ABI_INCLUDED",analyze(self.path)["problems"])
    def test_missing_arm32(self):
        self.apk({"assets/model-es/am/model":b"x","lib/arm64-v8a/libvosk.so":b"y"})
        self.assertIn("ARM32_NOT_PRESENT",analyze(self.path)["problems"])
    def test_missing_voice(self):
        self.apk({"classes.dex":b"a"})
        self.assertIn("OFFLINE_SPANISH_MODEL_ABSENT",analyze(self.path)["problems"])
    def test_research_rejected(self):
        self.apk({"assets/model-es/am/model":b"x","training/foo.bin":b"y"})
        self.assertIn("RESEARCH_FILES_PACKAGED",analyze(self.path)["problems"])
    def test_budget(self):
        self.apk({"assets/model-es/am/model":b"x"*1000})
        self.assertIn("APK_OVER_BUDGET",analyze(self.path,.000001)["problems"])
    def test_no_ram_claim(self):
        self.apk({"assets/model-es/am/model":b"x"})
        self.assertFalse(analyze(self.path)["runtime_ram_measured"])
    def test_bad_file(self):
        with self.assertRaises(ValueError):analyze(self.path)
if __name__=="__main__":unittest.main()
