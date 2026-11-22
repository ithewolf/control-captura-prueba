package com.iawolf.captureprobe;

public final class PiperPlaybackBudgetSmokeTest {
 private static void check(boolean value,String msg){
  if(!value)throw new AssertionError(msg);
 }
 public static void main(String[] args){
  check(PiperPlaybackBudget.drainMillis(24000,0,24000)==3000,"short PCM budget");
  check(PiperPlaybackBudget.drainMillis(60L*24000,0,24000)>=60000,"one-minute speech must not clip");
  check(PiperPlaybackBudget.drainMillis(120L*24000,60L*24000,24000)>=60000,"remaining audio length");
  check(PiperPlaybackBudget.drainMillis(100L*24000,100L*24000,24000)==2000,"drained head");
  check(PiperPlaybackBudget.drainMillis(3600L*24000,0,24000)==180000,"watchdog safety");
  boolean rejected=false;
  try{PiperPlaybackBudget.drainMillis(24000,0,0);}catch(IllegalArgumentException good){rejected=true;}
  check(rejected,"reject invalid sample rate");
  System.out.println("Lab4 playback budget tests PASS; real Android playback still unverified");
 }
}
