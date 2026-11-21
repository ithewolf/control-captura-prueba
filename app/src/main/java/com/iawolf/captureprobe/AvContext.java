package com.iawolf.captureprobe;

import java.util.Locale;

/**
 * Causal audio/video bridge. Stores metadata only (no voice samples or frames).
 * Clock is SystemClock.elapsedRealtime() for BOTH incoming streams.
 * Never claims game awareness from a hash/change counter.
 */
public final class AvContext {
 private AvContext() {}
 public static final long MAX_FRAME_AGE_MS = 2500L;
 private static long lastFrameAt = -1L;
 private static long sessionId = 0L;
 private static long lastFrameNumber = 0L;
 private static long lastChangeNumber = 0L;
 private static boolean lastDark = true;
 private static boolean capturing = false;

 public static synchronized void reset(long newSessionId) {
  sessionId = newSessionId;
  lastFrameAt = -1L; lastFrameNumber = 0L;
  lastChangeNumber = 0L; lastDark = true; capturing = false;
 }

 public static synchronized void recordFrame(long session, long clockMs,
        long frameNumber, long changes, boolean dark) {
  if (session != sessionId || clockMs < lastFrameAt) return;
  lastFrameAt = clockMs; lastFrameNumber = frameNumber;
  lastChangeNumber = changes; lastDark = dark; capturing = true;
 }

 public static synchronized Snapshot snapshot(long session, long spokenAtMs) {
  if (session != sessionId || !capturing || lastFrameAt < 0L ||
      spokenAtMs < lastFrameAt || spokenAtMs - lastFrameAt > MAX_FRAME_AGE_MS)
   return new Snapshot(false, 0L, 0L, false, -1L);
  return new Snapshot(true, lastFrameNumber, lastChangeNumber,
    lastDark, spokenAtMs - lastFrameAt);
 }

 public static final class Snapshot {
  public final boolean fresh;
  public final long frameNumber, changes, ageMs;
  public final boolean dark;
  Snapshot(boolean fresh,long frame,long changes,boolean dark,long age) {
   this.fresh=fresh;frameNumber=frame;this.changes=changes;this.dark=dark;ageMs=age;
  }
 }

 /** Diagnostic speech, not tactical advice. */
 public static String respond(String phrase, Snapshot s) {
  if (phrase == null) return null;
  String text = java.text.Normalizer.normalize(phrase, java.text.Normalizer.Form.NFD)
    .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
  // Exact wake word at the beginning (no background chatter or substrings).
  // A wake word alone is not enough: require a following command.
  if (!text.matches("^\\s*control\\b[\\s,.:;!?-]+.+")) return null;
  if (!s.fresh)
   return "Wolf, te escuché, pero no tengo una imagen reciente del juego. No voy a inventar lo que está pasando.";
  if (s.dark)
   return "Wolf, te escuché, pero la imagen reciente está oscura. No puedo analizar la situación.";
  if (text.contains("estado") || text.contains("ves") || text.contains("pantalla"))
   return "Wolf, te escucho y recibí una imagen reciente. Todavía no reconozco campeones ni situaciones tácticas.";
  return "Te escuché, Wolf. Tengo una imagen reciente, pero todavía no puedo darte un consejo táctico confiable.";
 }
}
