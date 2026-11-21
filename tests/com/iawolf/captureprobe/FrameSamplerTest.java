package com.iawolf.captureprobe;
import java.nio.ByteBuffer;
public final class FrameSamplerTest {
 private static int passed=0;
 private static void ok(boolean c,String name){if(!c)throw new AssertionError(name);passed++;}
 private static void invalid(Runnable action){try{action.run();throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){passed++;}}
 private static ByteBuffer frame(int w,int h,int row,int color){
  ByteBuffer b=ByteBuffer.allocate(row*h);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int p=y*row+x*4;
   b.put(p,(byte)((color>>16)&255));b.put(p+1,(byte)((color>>8)&255));
   b.put(p+2,(byte)(color&255));b.put(p+3,(byte)255);
  }
  return b;
 }
 public static void main(String[] args){
  var black=FrameSampler.sample(frame(320,180,1280,0),320,180,1280,4);
  ok(black.dark,"black marked dark");
  var white=FrameSampler.sample(frame(320,180,1280,0xffffff),320,180,1280,4);
  ok(!white.dark,"white marked visible");
  ok(black.fingerprint!=white.fingerprint,"frame changes detected");
  ok(white.fingerprint==FrameSampler.sample(frame(320,180,1280,0xffffff),320,180,1280,4).fingerprint,"stable content");
  var padded=FrameSampler.sample(frame(320,180,1336,0xffffff),320,180,1336,4);
  ok(padded.fingerprint==white.fingerprint,"row padding ignored");
  var vertical=FrameSampler.sample(frame(180,320,720,0xffffff),180,320,720,4);
  ok(!vertical.dark,"portrait supported");
  invalid(()->FrameSampler.sample(ByteBuffer.allocate(10),320,180,1280,4));
  invalid(()->FrameSampler.sample(frame(4,4,16,0),0,4,16,4));
  invalid(()->FrameSampler.sample(frame(4,4,16,0),4,4,8,4));
  invalid(()->FrameSampler.sample(frame(4,4,16,0),4,4,16,3));
  System.out.println("OK: "+passed+" pruebas FrameSampler");
 }
}
