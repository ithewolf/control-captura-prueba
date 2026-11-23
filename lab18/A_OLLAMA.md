# Lab18A — Ollama local (PC)

Actual prototype: `tools/lab18a_ollama.py` sends identical open-answer questions from Lab17 to Ollama `/api/chat` using structured JSON response format and records each real answer + latency. This does **not** turn synthetic rule tests into LLM scores.

Run only on a machine with a locally installed model: `python3 tools/lab18a_ollama.py --model control-v3 --out lab18a_results.json`; substitute the actual installed Ollama model name after `ollama list`. No paid endpoint or cloud access. GitHub Actions intentionally only tests the adapter; hosted runners do not have user models. Validate real inference on PC before comparing.

Source: https://docs.ollama.com/capabilities/structured-outputs
