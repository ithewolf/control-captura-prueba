#!/usr/bin/env python3
"""Offline video inventory and honest enemy-identity evaluation.

This script NEVER infers a champion label from a filename.
Without independent human-reviewed labels, recognition metrics are NOT_MEASURED.
Source videos stay local and are never uploaded by this script.
"""
import argparse
import json
import pathlib
import subprocess
import csv
import math
from collections import defaultdict

SAMPLES = (30, 60, 90, 120, 180, 240, 360)
REQUIRED = {"video_id", "time_s", "champion_id", "team"}

def read_json(path):
    return json.loads(pathlib.Path(path).read_text(encoding="utf-8"))

def validate_manifest(manifest):
    videos = manifest["videos"]
    if len(videos) != 5 or len({v["id"] for v in videos}) != 5:
        raise ValueError("Expected five different video IDs")
    for v in videos:
        if not v["file"].lower().endswith(".mp4") or "/" in v["file"] or "\\" in v["file"]:
            raise ValueError("Video names must be local MP4 basenames")
        if v["split"] not in {"development", "held_out"}:
            raise ValueError("Invalid split")
    if not any(v["split"] == "held_out" for v in videos):
        raise ValueError("Missing held-out sources")
    return videos

def probe(path):
    cmd = ["ffprobe", "-v", "error", "-select_streams", "v:0",
           "-show_entries", "stream=width,height,r_frame_rate:format=duration",
           "-of", "json", str(path)]
    result = subprocess.run(cmd, capture_output=True, text=True, check=True, timeout=60)
    data = json.loads(result.stdout)
    stream = data["streams"][0]
    return {"width": stream["width"], "height": stream["height"],
            "fps": stream.get("r_frame_rate"), "duration_s": float(data["format"]["duration"])}

def extract(video_path, out_dir, moments):
    out_dir.mkdir(parents=True, exist_ok=True)
    records = []
    for second in moments:
        out = out_dir / ("frame_%06d.jpg" % second)
        cmd = ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
               "-ss", str(second), "-i", str(video_path), "-frames:v", "1",
               "-vf", "scale=640:-2", str(out)]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=90)
        records.append({"time_s": second, "frame": str(out) if out.is_file() else None,
                        "extracted": result.returncode == 0 and out.is_file()})
    return records

def records_csv(path):
    with pathlib.Path(path).open(encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))

def identity(row):
    # Annotators and detectors must provide independent outputs; filenames are ignored.
    if not REQUIRED.issubset(row):
        raise ValueError("Missing required column")
    if row["team"] not in {"ENEMY", "ALLY", "UNKNOWN"}:
        raise ValueError("Invalid team label")
    if not math.isfinite(float(row["time_s"])) or float(row["time_s"]) < 0:
        raise ValueError("Invalid timestamp")
    return (row["champion_id"].strip().lower(), row["team"])

def evaluate(labels, predictions, split_map):
    """One-to-one scored matches prevent repeated predictions inflating recall.

    Identity errors count as both missed ground truth and a false prediction.
    A negative annotation (ALLY/UNKNOWN) is still an independently reviewed
    frame, not proof that all unannotated frames contain no enemies.
    """
    if labels is None or predictions is None:
        return {"status": "NOT_MEASURED", "reason": "Reviewed labels and detector predictions required"}

    reviewed = defaultdict(list)
    detected = defaultdict(list)
    for label in labels:
        if label.get("video_id") not in split_map:
            raise ValueError("Unknown video_id in human-reviewed labels")
        identity(label)
        reviewed[label["video_id"]].append(label)
    for pred in predictions:
        vid = pred.get("video_id")
        if vid not in split_map:
            raise ValueError("Unknown video_id in detector predictions")
        ident = identity(pred)
        confidence = float(pred.get("confidence", "nan"))
        if not math.isfinite(confidence) or not 0 <= confidence <= 1:
            raise ValueError("Invalid prediction confidence")
        if confidence >= .80 and ident[1] == "ENEMY" and ident[0]:
            detected[vid].append(pred)

    results = {}
    for split in ("development", "held_out"):
        tp = fp = fn = 0
        for vid, video_split in split_map.items():
            if video_split != split:
                continue
            ground_truth = [r for r in reviewed[vid] if identity(r)[1] == "ENEMY"]
            predictions_here = sorted(detected[vid], key=lambda r: -float(r["confidence"]))
            used_labels = set()
            for prediction in predictions_here:
                same_identity = [
                    (abs(float(prediction["time_s"]) - float(label["time_s"])), idx)
                    for idx, label in enumerate(ground_truth)
                    if idx not in used_labels
                    and identity(label) == identity(prediction)
                    and abs(float(prediction["time_s"]) - float(label["time_s"])) <= .5
                ]
                if same_identity:
                    used_labels.add(min(same_identity)[1])
                    tp += 1
                else:
                    fp += 1
            fn += len(ground_truth) - len(used_labels)
        results[split] = {
            "true_positive": tp, "false_positive": fp, "false_negative": fn,
            "precision": round(tp/(tp+fp), 4) if tp+fp else None,
            "recall": round(tp/(tp+fn), 4) if tp+fn else None,
            "note": "One-to-one time-and-identity matches to independently human-reviewed labels",
            "limitation": "Unreviewed timestamps cannot be treated as verified negatives",
        }
    return {"status": "MEASURED", "splits": results}

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--manifest", default="tools/lab9_manifest.json")
    p.add_argument("--video-root", type=pathlib.Path)
    p.add_argument("--output", type=pathlib.Path, default=pathlib.Path("lab9_results"))
    p.add_argument("--labels", type=pathlib.Path)
    p.add_argument("--predictions", type=pathlib.Path)
    args = p.parse_args()
    sources = validate_manifest(read_json(args.manifest))
    args.output.mkdir(parents=True, exist_ok=True)
    inventory = []
    for video in sources:
        item = {"id": video["id"], "source": video["file"], "split": video["split"]}
        path = args.video_root / video["file"] if args.video_root else None
        if path is None or not path.is_file():
            item["status"] = "SOURCE_NOT_MOUNTED"
        else:
            item["metadata"] = probe(path)
            moments = [s for s in SAMPLES if s < item["metadata"]["duration_s"] - 1]
            item["samples"] = extract(path, args.output / video["id"], moments)
            item["status"] = "SAMPLED"
        inventory.append(item)
    split_map = {v["id"]: v["split"] for v in sources}
    metrics = evaluate(records_csv(args.labels) if args.labels else None,
                       records_csv(args.predictions) if args.predictions else None, split_map)
    report = {"schema": 1, "source_count": len(sources),
              "video_inventory": inventory, "recognition_evaluation": metrics,
              "warning": "Frame extraction does not prove champion recognition; no filename labels."}
    report_path = args.output / "evaluation.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps({"report": str(report_path), "recognition_status": metrics["status"],
                      "videos_mounted": sum(i["status"] == "SAMPLED" for i in inventory)}))
if __name__ == "__main__":
    main()
