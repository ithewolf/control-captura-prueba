import unittest
from unittest.mock import patch
from tools.lab18c_hybrid import gate,bench,FIELDS
def answer(decision):return {f:(decision if f=="decision" else f+" comprobado") for f in FIELDS}
class HybridTests(unittest.TestCase):
    def test_no_data_blocks_aggression(self):
        self.assertEqual(gate(answer("Atacar ahora"),{})["reason"],"UNSUPPORTED_OFFENSIVE_DECISION")
    def test_unverified_observations_block(self):
        self.assertFalse(gate(answer("Esperar"),{})["safe"])
    def test_verified_context_and_non_offense(self):
        s=gate(answer("Observar y mantener posicion"),{"reliable":True,"fresh":True})
        self.assertTrue(s["safe"])
    def test_confirmed_enemy_without_escape_blocks_offense(self):
        s=gate(answer("Atacar"),{"reliable":True,"fresh":True,
              "enemy_champion_confirmed":True,"escape_route_verified":False})
        self.assertFalse(s["safe"])
    def test_malformed_schema_blocks(self):
        self.assertFalse(gate({"decision":"Atacar"},{"reliable":True,"fresh":True})["safe"])
    @patch('tools.lab18c_hybrid.ask',side_effect=OSError('model offline'))
    def test_offline_no_fake_result(self,call):
        x=bench([{"id":"q","prompt":"situation"}],"m","http://127.0.0.1:11434",5)
        self.assertIsNone(x[0]["answer"])
if __name__=="__main__":unittest.main()
