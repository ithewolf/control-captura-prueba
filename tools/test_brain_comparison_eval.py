import unittest
from tools.brain_comparison_eval import validate,audit
def good():
    return {k:k+" actual explanation" for k in ("decision","reason","evidence","risk","alternative","contingency","uncertainty")}
class ComparisonTests(unittest.TestCase):
    def test_not_run_is_not_approved(self):
        a=audit([{"id":"q1","answer":None}]);self.assertEqual(a["status"],"NO_MODEL_INFERENCE")
        self.assertIsNone(a["schema_pass_percent"])
    def test_valid_schema_is_not_strategy_accuracy(self):
        a=audit([{"id":"q1","answer":good(),"latency_ms":120}])
        self.assertEqual(a["strategic_correctness"],"NOT_MEASURED")
        self.assertEqual(a["schema_pass_percent"],100)
    def test_missing_field(self):
        a=good();a.pop("uncertainty");self.assertIn("missing_or_invalid_uncertainty",validate(a))
    def test_not_json(self):self.assertEqual(validate("hello"),["not_json_object"])
    def test_latency_malformed_ignored(self):
        a=audit([{"id":"q1","answer":good(),"latency_ms":-4}])
        self.assertIsNone(a["latency_mean_ms"])
if __name__=="__main__":unittest.main()
