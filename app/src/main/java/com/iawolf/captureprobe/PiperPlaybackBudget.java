package com.iawolf.captureprobe;

/** Pure-Java playback drain watchdog policy. The Android AudioTrack playback
 * cursor, not the PCM write completion, determines when Vosk may resume.
 * This is a timeout BUDGET only, not a claim that audible playback occurred.
 */
final class PiperPlaybackBudget {
 private PiperPlaybackBudget(){}
 static long drainMillis(long totalSamples,long alreadyPlayed,int sampleRate){
  if(totalSamples<=0||alreadyPlayed<0||sampleRate<8000)
   throw new IllegalArgumentException("invalid PCM frames");
  long remaining=Math.max(0,totalSamples-alreadyPlayed);
  // Old 45s cap incorrectly aborted otherwise valid long replies.
  // Keep a safety timeout: allow replies up to 3 minutes, with margin.
  long ms=(remaining*1000L)/sampleRate+2000L;
  return Math.min(180000L,Math.max(2000L,ms));
 }
}
