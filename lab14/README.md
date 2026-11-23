# CONTROL Lab14 — 7 real-video review windows (NO APK)

Lab12 decoded the user-provided Yasuo A video and sampled 90 frames (89 difference measurements). Lab14 derives **seven candidate review intervals** from the 85th percentile (threshold 33.1557). Peak timestamps: **2, 10, 30, 40, 52, 136, 154 seconds**. See `yasuo_a_motion_review.json`.

**What passed locally:** seven unit checks of thresholding, input validation, duplicate timestamps and the prohibition on event claims. The video data was measured independently in a local analysis session. The source video remains in the user's ChatGPT Library, not GitHub.

These are **review candidates, NOT recognized fights/retreats/ganks**. Visual change alone is not evidence of a particular strategic event. The gameplay perspective/version must be confirmed before using the sample as Wild Rift ground truth. Lab13 can consume reliable detector output later; it must abstain on motion-only data.

Next: label each window from video context, run an independent detector, compare predictions on held-out videos, then connect proven detections to A/B/C decision reports. Keep Android APK compilation suspended.

Usage: `python3 tools/lab14_review_windows.py --input lab12_report.json --out review_windows.json`.
