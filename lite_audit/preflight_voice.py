"""Preflight rules protecting offline voice while preventing research tools in APK."""
from pathlib import Path

def inspect_gradle(gradle):
    text=Path(gradle).read_text(encoding="utf-8")
    problems=[]
    if "com.alphacephei:vosk-android:" not in text:problems.append("VOSK_MISSING")
    if "net.java.dev.jna:jna:" not in text:problems.append("JNA_MISSING")
    if "vosk-model-small-es-0.42.zip" not in text:problems.append("SPANISH_MODEL_NOT_CONFIGURED")
    for item in ("ultralytics","cvat","mmdetection","mmaction","opencv-python","torch"):
        if item in text.lower():problems.append("TRAINING_DEPENDENCY:"+item)
    return {"status":"PASS" if not problems else "FAIL","problems":problems}
