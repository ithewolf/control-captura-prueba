# Lab18C — CONTROL hybrid brain (local model + hard tactical guards)

Architecture: actual local Ollama generative model returns structured reasoning, plus deterministic evidence gate blocks unsupported attacks and invalid/no-observation recommendations. Same 20 questions, same shared evaluator as Lab18A/B. The gate does **not** replace `TacticalDecisionEngine`: current code is a prototype safety adapter; the actual Java engine integration is a later task. No cross-game memory from Project Luna.

Run when Ollama model is installed locally: `python3 tools/lab18c_hybrid.py --model control-v3 --out lab18c_results.json`.
On questions with no validated observations, the guard must be conservative. Compare with A using the identical model, then blind-grade the answers; the guard's pass-rate is not strategic accuracy. Hosted Actions tests only guards (model absent), NO APK.

Future: pipe trustworthy, fresh Android detector observations to existing `TacticalPlanCoordinator` and compare native results with generated advice before speech. Model should never override a critical retreat recommendation.

Sources: https://docs.ollama.com/capabilities/structured-outputs
