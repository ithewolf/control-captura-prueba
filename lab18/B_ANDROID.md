# Lab18B — full local model on Android (NO APK)
This bench uses Android Debug Bridge to invoke an **actual arm64 Android llama.cpp executable** with locally provisioned GGUF model and record raw responses/latency. It is not an APK, doesn't upload user data, and has no paid API. Requires connecting a real authorized Android phone and installing binary/model manually under `/data/local/tmp/wolf`. Validate the binary, supported flags, and Android permission context on the target phone.

Run on a PC with ADB and the phone connected: `python3 tools/lab18b_android.py --binary /data/local/tmp/wolf/llama-cli --model /data/local/tmp/wolf/model.gguf`. If no device/model: report `ADB_DEVICE_UNAVAILABLE` or `MODEL_MISSING_ON_DEVICE`, not a measured result.

This tests an on-device runtime externally, **not integration inside CONTROL APK**. On-device thermal throttling, memory and live MediaProjection have NOT been assessed. B comparison is blocked until real device/model are available.

Source: https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android
