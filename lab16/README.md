# CONTROL Lab16 — 15 REAL Yasuo frames ready for human labeling

15 JPGs from the user's locally available Yasuo gameplay file, timestamped 2,4,10,28,30,38,40,42,52,76,100,120,136,154,158 s, were extracted using ffmpeg to a separate ZIP in the ChatGPT conversation. **Images are NOT committed to GitHub**. The separate ZIP includes a CSV with SHA256 hashes, frame timestamps and all frames assigned to development only. They are **not labeled** and no game or view verification is claimed.

Next steps: manually confirm that source is Wild Rift and perspective/version match Android; annotate boxes in CVAT for SELF, ALLY_CHAMPION, ENEMY_CHAMPION, ALLY_MINION, ENEMY_MINION, TOWER, DRAGON, BARON, plus reviewed NO_GAMEPLAY scene status. NEVER label champions based on filenames or copy predictions as ground truth.

**Anti-leakage rule:** all 15 frames from Yasuo A are development-only. A future held-out dataset must use an independent source video; exporting an image-level random split of Yasuo A is forbidden for acceptance testing.\n\nThe YOLO Detection format documented at https://docs.cvat.ai/docs/manual/advanced/formats/format-yolo-ultralytics/ stores one normalized class-center-width-height per row. The audit enforces class/coordinate ranges, source verification, a distinct video-level holdout and valid reviewed labels. It must block the real exported ZIP until annotation is completed.

A trained detector and measured accuracy are NOT YET AVAILABLE. Class labels here are a requested taxonomy, not findings in frames.

Run: `python3 -m unittest tools.test_lab16_dataset_audit -v`, then `python3 tools/lab16_dataset_audit.py --manifest ... --images ... --labels ... --out ...`.
