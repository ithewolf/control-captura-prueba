import unittest
from unittest.mock import patch
from tools.lab18a_ollama import run
class OllamaTests(unittest.TestCase):
    @patch('tools.lab18a_ollama.call',side_effect=OSError('offline'))
    def test_offline_produces_no_fabricated_response(self,fn):
        r=run([{"id":"x","prompt":"test"}],"local","http://127.0.0.1:11434",2)
        self.assertIsNone(r[0]["answer"])
    @patch('tools.lab18a_ollama.call',return_value=({"decision":"esperar"},123))
    def test_returns_actual_received_answer(self,fn):
        r=run([{"id":"x","prompt":"test"}],"local","http://127.0.0.1:11434",2)
        self.assertEqual(r[0]["answer"]["decision"],"esperar")
    def test_empty_questions(self):
        self.assertEqual(run([],"m","http://127.0.0.1:11434",2),[])
if __name__=="__main__":unittest.main()
