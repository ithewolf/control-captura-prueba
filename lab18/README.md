# CONTROL — Shared and fair evaluation harness for three brain labs

Create A (Ollama on PC), B (local Android), and C (hybrid) from this common base, using the **same 20 open-answer scenarios** in `lab17/open_response_20_questions.json`, identical seven required fields, no answer options, no hidden answer key in the prompt.

- All labs must save model name/version, execution platform, raw answers, total/first-token latency if measured, quality evaluation by independent human reviewer, memory/RAM/energy **only if observed**.
- If model is not present or Android runtime does not run, mark inference `NO_MODEL_INFERENCE`. Never call unit-test successes 'brain intelligence'.
- Schema validity is a separate axis from strategic accuracy.
- Scores for strategic correctness require blind review of evidence, appropriate plans, uncertainty, and consequences.
- For option C, compare factual compliance and caution against A with same model and prompts, noting additional deterministic constraints.
- No APK build, paid API, web upload of user videos, or cross-contamination of Luna/Lineage 2 memory.
