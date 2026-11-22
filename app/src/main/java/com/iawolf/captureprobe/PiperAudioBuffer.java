package com.iawolf.captureprobe;

/** PCM16 mono AudioTrack buffer sizing. The Android minimum is in BYTES,
 * not samples. Keep byte length aligned to 2 bytes/sample.
 */
final class PiperAudioBuffer {
 private PiperAudioBuffer(){}
 static int bytes(int sampleRate,int minBufferBytes){
  if(sampleRate<8000||sampleRate>192000||minBufferBytes<=0)
   throw new IllegalArgumentException("bad AudioTrack rate/minimum");
  long proposed=Math.max((long)minBufferBytes,2L*sampleRate);
  long aligned=(proposed+1L)&~1L;
  if(aligned>Integer.MAX_VALUE)
   throw new IllegalArgumentException("audio buffer overflow");
  return (int)aligned;
 }
}
