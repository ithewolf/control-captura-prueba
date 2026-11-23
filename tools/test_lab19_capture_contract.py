"""CI tests for source contract: make no claim that the phone was tested."""
import unittest
from tools.lab19_capture_contract import audit,BASE

class CaptureContractTests(unittest.TestCase):
 def test_source_contract(self):
  self.assertTrue(audit(BASE)["contract_passed"])
 def test_real_device_is_unverified(self):
  self.assertFalse(audit(BASE)["unverified_runtime_requirements"]["real_device_capture_verified"])
 def test_vision_integration_is_unverified(self):
  self.assertFalse(audit(BASE)["unverified_runtime_requirements"]["gameplay_image_delivered_to_vision_model"])
 def test_throttle_is_not_realtime_inference(self):
  self.assertIn("900-ms",audit(BASE)["evidence"]["max_processing_rate"])
 def test_all_checks_are_boolean(self):
  self.assertTrue(all(type(x) is bool for x in audit(BASE)["source_contract"].values()))
if __name__=="__main__":unittest.main()
