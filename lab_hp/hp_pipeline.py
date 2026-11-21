"""Research-only CVAT -> custom YOLO -> OpenCV bridge. No tactical advice."""
import cv2
import numpy as np
import math

def measure_fill(image, box):
    """Measure candidate colored width ONLY inside a confirmed full-track box.

    Returns unknown for invalid geometry, missing fill or incompatible input.
    This is not a validated health percentage.
    """
    if not isinstance(image, np.ndarray) or image.ndim != 3 or image.shape[2] != 3:
        return {"fraction": None, "reason": "invalid_frame"}
    if not isinstance(box, (list, tuple, np.ndarray)) or len(box) != 4:
        return {"fraction": None, "reason": "invalid_box"}
    try:
        coords = [float(v) for v in box]
    except (ValueError, TypeError, OverflowError):
        return {"fraction": None, "reason": "invalid_box"}
    if not all(math.isfinite(v) for v in coords):
        return {"fraction": None, "reason": "invalid_box"}
    x1, y1, x2, y2 = map(int, coords)
    h, w = image.shape[:2]
    if not (0 <= x1 < x2 <= w and 0 <= y1 < y2 <= h
            and x2 - x1 >= 12 and y2 - y1 >= 2):
        return {"fraction": None, "reason": "invalid_box"}
    pad = 1 if y2 - y1 >= 5 else 0
    crop = image[y1 + pad:y2 - pad if pad else y2,
                 x1 + pad:x2 - pad if pad else x2]
    if crop.size == 0:
        return {"fraction": None, "reason": "empty_crop"}
    hsv = cv2.cvtColor(crop, cv2.COLOR_BGR2HSV)
    hue = hsv[:, :, 0]
    saturated = (hsv[:, :, 1] >= 75) & (hsv[:, :, 2] >= 75)
    possible_colors = ((hue >= 35) & (hue <= 88)) | (hue <= 25) | (hue >= 170)
    columns = np.mean(saturated & possible_colors, axis=0) >= .4
    active = np.flatnonzero(columns)
    if len(active) == 0:
        return {"fraction": None, "reason": "no_colored_fill"}
    if active[0] > max(3, int(len(columns) * .14)):
        return {"fraction": None, "reason": "not_left_anchored"}
    if np.mean(columns[active[0]:active[-1] + 1]) < .77:
        return {"fraction": None, "reason": "discontinuous"}
    return {"fraction": round(float(active[-1] + 1) / len(columns), 3),
            "reason": "candidate_only", "health_percent": None}


def analyze(image, predictions, threshold=.6):
    """Fail closed on malformed detections, never forward uncertain game state."""
    output = []
    if not isinstance(predictions, (list, tuple)) or not math.isfinite(threshold):
        return {"observations": [], "game_state": "UNKNOWN",
                "ready_for_android": False}
    for prediction in predictions:
        if not isinstance(prediction, dict) or prediction.get("label") != "health_bar_track":
            continue
        try:
            confidence = float(prediction.get("confidence", 0))
        except (TypeError, ValueError, OverflowError):
            continue
        if not math.isfinite(confidence) or not (0 <= confidence <= 1) or confidence < threshold:
            continue
        box = prediction.get("xyxy")
        measurement = measure_fill(image, box)
        if measurement["fraction"] is None:
            continue
        output.append({"xyxy": box, "confidence": confidence,
                       **measurement, "status": "unvalidated_candidate"})
    return {"observations": output, "game_state": "UNKNOWN",
            "ready_for_android": False}

def predict_with_yolo(image,custom_weights):
    try:from ultralytics import YOLO
    except ImportError as e:raise RuntimeError("Install optional Ultralytics in an isolated environment") from e
    model=YOLO(custom_weights)
    outputs=[]
    for result in model.predict(source=image,verbose=False):
        for box in result.boxes:
            outputs.append({"label":result.names[int(box.cls[0])],
                            "confidence":float(box.conf[0]),
                            "xyxy":[int(v) for v in box.xyxy[0].tolist()]})
    return outputs
