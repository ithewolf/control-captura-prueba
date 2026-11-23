import unittest
from unittest.mock import patch
from tools.lab18b_android import check_device,safe_path,run_cmd
class AndroidTests(unittest.TestCase):
    def test_no_injection(self):
        self.assertFalse(safe_path('/data/local/tmp/wolf/model;rm'))
        self.assertFalse(safe_path('/sdcard/model.gguf'))
        self.assertTrue(safe_path('/data/local/tmp/wolf/model.gguf'))
    @patch('tools.lab18b_android.subprocess.run', side_effect=FileNotFoundError)
    def test_missing_adb_graceful(self,runner):
        result=run_cmd(['adb','get-state'])
        self.assertEqual(result.returncode,127)
        self.assertEqual(result.stderr,'ADB_MISSING_OR_TIMEOUT')
    @patch('tools.lab18b_android.run_cmd')
    def test_no_device_not_success(self,cmd):
        cmd.return_value.returncode=1
        cmd.return_value.stdout=''
        self.assertEqual(check_device('a','b')[1],'ADB_DEVICE_UNAVAILABLE')
    @patch('tools.lab18b_android.run_cmd')
    def test_architecture_mismatch(self,cmd):
        cmd.side_effect=[type('R',(),{'returncode':0,'stdout':'device'})(),
                         type('R',(),{'returncode':0,'stdout':'armeabi-v7a'})()]
        self.assertEqual(check_device('a','b')[1],'ARM64_REQUIRED')
if __name__=='__main__':unittest.main()
