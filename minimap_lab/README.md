# CONTROL minimap evaluator (offline, read-only)
Run: `python -m unittest discover -s minimap_lab/tests -v`
Run with real manually labeled frames: `python -m minimap_lab.evaluate minimap_lab/dataset/labels.json --report output/minimap_eval.json`

Dataset JSON schema:
```json
{"schema":"control-minimap-template-eval-v1","samples":[
{"frame":"frames/01.png","template":"templates/icon.png","expected_present":true,"expected_x":12,"expected_y":24},
{"frame":"frames/02.png","template":"templates/icon.png","expected_present":false}
]}
```
Images are grayscale or RGB PNG, cropped to max 320x320. All paths relative to the dataset JSON directory. Do not commit private videos or screenshots of chat. Use multiple independent matches, champions, HUD layouts, darkness and occlusions; do not split near-duplicate frames from a single match across train/test.

This tool deliberately reports only icon candidate precision/recall. It does not know whether the icon depicts an ally, enemy, champion or objective. False-positive results must be assessed before any tactical integration. Synthetic unit tests validate geometry and scoring, not gameplay accuracy. Keep training assets licensed and locally stored.
