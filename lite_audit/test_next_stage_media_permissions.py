"""Lab3 next-stage static guard for simultaneous Android screen and microphone requirements.
This checks declarations, not actual granted permissions on a Samsung.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ANDROID="{http://schemas.android.com/apk/res/android}"
ROOT=Path(__file__).resolve().parents[1]
class AndroidCaptureContract(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.root=ET.parse(ROOT/"app/src/main/AndroidManifest.xml").getroot()
 def test_permissions_declared(self):
  perms={x.attrib.get(ANDROID+"name") for x in self.root.findall("uses-permission")}
  for required in ("android.permission.RECORD_AUDIO",
                   "android.permission.FOREGROUND_SERVICE_MICROPHONE",
                   "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"):
   self.assertIn(required,perms)
 def test_service_declares_both_fg_types(self):
  svc=self.root.find(".//service[@"+ANDROID+"name"+"='.CaptureService']")
  self.assertIsNotNone(svc)
  t=set(svc.attrib.get(ANDROID+"foregroundServiceType","").split("|"))
  self.assertEqual(t,{"mediaProjection","microphone"})
  self.assertEqual(svc.attrib.get(ANDROID+"exported"),"false")
 def test_no_network_requirement_for_capture(self):
  perms={x.attrib.get(ANDROID+"name") for x in self.root.findall("uses-permission")}
  self.assertNotIn("android.permission.INTERNET",perms)
