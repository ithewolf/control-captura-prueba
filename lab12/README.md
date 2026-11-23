# CONTROL Lab12: real video visual-change baseline — NO APK

The baseline reads a real local video using OpenCV. It measures mean frame-to-frame
change in a central crop at 2-second intervals. **It cannot classify tactics**.
No video, frame or player profile is committed to GitHub.

## Directly executed result
Source in user's Library: `TOP 1 YASUO SERVER “JarePley”🐲 WILD RIFT.mp4`.
Machine test: 60 FPS, 179.95 s, 90 samples, mean absolute visual change 28.0475.
Some high-change samples at 2, 4, 30, 40, 42, 136, 154 and 156 seconds.
High scores can be caused by camera cuts, camera pans, effects or gameplay.
Actual game events remain `NOT_MEASURED`.

Command with an authorized local MP4:
```bash
python3 -m pip install opencv-python-headless
python3 tools/lab12_video_motion_baseline.py --video /local/authorized.mp4 --out /tmp/lab12.json
```

## Next acceptance gate
1. Confirm video belongs to intended game and gameplay perspective.
2. Human-annotate short temporal intervals around candidate moments.
3. Build INDEPENDENT event detector, output predictions CSV.
4. Score against labels with Lab11. Never use the same labels as detector outputs.
5. Feed ONLY reliable observations into the tactical planner; verify safe recommendations.
6. Confirm Android capture separately; **do not compile APK**.

This is a diagnostic instrument, not a trained vision model.
