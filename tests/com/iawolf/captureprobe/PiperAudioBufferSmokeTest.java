package com.iawolf.captureprobe;
public final class PiperAudioBufferSmokeTest {
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 public static void main(String[] args){
  check(PiperAudioBuffer.bytes(22050,5000)==44100,"22050 Hz PCM16 is 44100 bytes/s");
  check(PiperAudioBuffer.bytes(24000,60000)==60000,"honor larger Android minimum");
  check(PiperAudioBuffer.bytes(22050,44101)==44102,"align odd minimum to 2-byte samples");
  check(PiperAudioBuffer.bytes(48000,20000)==96000,"48000 Hz mono PCM16");
  boolean bad=false;
  try{PiperAudioBuffer.bytes(24000,-2);}catch(IllegalArgumentException ex){bad=true;}
  check(bad,"reject AudioTrack ERROR code");
  System.out.println("PCM16 mono buffer byte/frame sizing PASS; actual playback on device not certified");
 }
}
