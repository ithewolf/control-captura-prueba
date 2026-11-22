package com.iawolf.captureprobe;

/**
 * Explicit, bounded sampling for opt-in gameplay evidence.
 * Only landscape frames qualify. Never captures automatically unless enabled.
 * This is a timing policy, NOT a Wild Rift detector.
 */
public final class GameFrameSelectionPolicy {
 public static final int MAX_SAMPLES=3;
 public static final long MIN_GAP_MS=4500L;
 private int count=0;
 private long lastSampleAt=-1L;

 public synchronized void reset(){count=0;lastSampleAt=-1L;}
 public synchronized int count(){return count;}
 public synchronized boolean finished(){return count>=MAX_SAMPLES;}
 public synchronized boolean shouldCapture(long now,int width,int height){
  if(now<0||width<height||width<=0||height<=0||finished())return false;
  if(lastSampleAt>=0 && (now<lastSampleAt||now-lastSampleAt<MIN_GAP_MS))return false;
  lastSampleAt=now;
  count++;
  return true;
 }
}
