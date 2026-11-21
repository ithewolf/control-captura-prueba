package com.iawolf.captureprobe;
import java.nio.ByteBuffer;
public final class FrameSampler {
 private FrameSampler(){}
 public static final class Result {
  public final long fingerprint; public final boolean dark;
  public Result(long f,boolean d){fingerprint=f;dark=d;}
 }
 public static Result sample(ByteBuffer b,int w,int h,int row,int pixel){
  if(b==null||w<1||h<1||pixel<4||row<(long)w*pixel)throw new IllegalArgumentException("geometry");
  long last=(long)(h-1)*row+(long)(w-1)*pixel+2;
  if(last>=b.limit())throw new IllegalArgumentException("buffer");
  long hash=0xcbf29ce484222325L;boolean lit=false;
  for(int yi=0;yi<6;yi++)for(int xi=0;xi<8;xi++){
   int x=(int)Math.min(w-1L,(long)(2*xi+1)*w/16);
   int y=(int)Math.min(h-1L,(long)(2*yi+1)*h/12);
   int i=y*row+x*pixel;
   int r=b.get(i)&255,g=b.get(i+1)&255,bl=b.get(i+2)&255;
   lit|=r+g+bl>36;
   hash^=((long)r<<16)|((long)g<<8)|bl;hash*=0x100000001b3L;
  }
  return new Result(hash,!lit);
 }
}