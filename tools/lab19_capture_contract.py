#!/usr/bin/env python3
"""Static capture contract audit. NO APK, NO Android runtime claim.

Inspects the actual main-branch capture probe source. Prints JSON audit.
"""
from pathlib import Path
import json
import re

BASE=Path("app/src/main")
def audit(base=BASE):
    service=(base/"java/com/iawolf/captureprobe/CaptureService.java").read_text()
    activity=(base/"java/com/iawolf/captureprobe/MainActivity.java").read_text()
    sampler=(base/"java/com/iawolf/captureprobe/FrameSampler.java").read_text()
    manifest=(base/"AndroidManifest.xml").read_text()
    checks={
        "manifest_media_projection_service":'android:foregroundServiceType="mediaProjection"' in manifest,
        "foreground_permission":'android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION' in manifest,
        "explicit_consent":'createScreenCaptureIntent()' in activity and 'onActivityResult' in activity,
        "projection_callback":'projection.registerCallback' in service and 'onStop()' in service,
        "image_reader":'ImageReader.newInstance' in service and 'acquireLatestImage()' in service,
        "always_close_images":'finally{if(img!=null)img.close();}' in service,
        "resize_path":'onCapturedContentResize' in service and 'display.resize' in service,
        "background_worker":'new HandlerThread("frames")' in service,
        "frame_statistics":'frames++' in service and 'changes++' in service and 'darkFrames++' in service,
        "sampled_pixel_data":'FrameSampler.sample' in service and 'ByteBuffer' in sampler,
        "projection_released":'projection=null;p.stop()' in service,
        "stop_foreground":'stopForeground(STOP_FOREGROUND_REMOVE)' in service,
    }
    # This probe only fingerprints 48 sample positions; it does not hand frames
    # to the vision model, and does not log timing distribution or identify Wild Rift.
    blockers={
        "gameplay_image_delivered_to_vision_model":False,
        "real_device_capture_verified":False,
        "frame_to_inference_latency_verified":False,
        "in_game_screen_switch_verified":False,
        "voice_with_gameplay_verified":False,
        "image_content_recognition_verified":False,
    }
    evidence={
        "sampling_grid":"8x6 = 48 image sampling locations per accepted frame",
        "max_processing_rate":"At most ~1.11 accepted samples/s by 900-ms throttle; not full-frame inference",
        "max_resolution":"longest side capped at 640 pixels",
        "capture_auto_stop":"600000 ms (10 min)",
        "warning":"Source presence is NOT a hardware or end-to-end capture test."
    }
    return {"source_contract":checks,
            "contract_passed":all(checks.values()),
            "unverified_runtime_requirements":blockers,
            "evidence":evidence,
            "next_device_gate":["Permission granted by user on Android",
                "Capture starts with persistent notification",
                "Open and switch to Wild Rift; frames and changes rise",
                "After returning to probe, record counters and errors",
                "Use separate gated implementation to pass image tensors to vision model"]}

if __name__=="__main__":
    result=audit()
    print(json.dumps(result,indent=2,ensure_ascii=False))
    if not result["contract_passed"]:
        raise SystemExit(1)
