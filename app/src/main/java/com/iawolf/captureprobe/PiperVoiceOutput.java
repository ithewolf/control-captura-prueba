package com.iawolf.captureprobe;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * LAB6-only compile-safe adapter: the experimental Sherpa/Piper engine belongs
 * to Lab4 and is not a dependency of the independent LiteRT-LM text experiment.
 *
 * When a saved setting requests Piper, report unavailable and allow
 * VoskVoiceBridge to fall back to an installed OFFLINE Android TTS voice.
 * Do not pretend that this adapter generates Piper audio.
 */
final class PiperVoiceOutput {
 interface Done { void completed(boolean ok, String reason); }
 private final Handler main = new Handler(Looper.getMainLooper());
 private boolean closed = false;

 PiperVoiceOutput(Context context) { }

 void speak(String words, Done callback) {
  main.post(() -> {
   if (!closed && callback != null) {
    callback.completed(false, "Piper no incluido en Lab6; laboratorio de voz preservado en Lab4");
   }
  });
 }

 void close() { closed = true; }
}
