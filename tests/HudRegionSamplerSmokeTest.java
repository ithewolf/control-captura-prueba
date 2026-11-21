package com.iawolf.captureprobe;
import java.nio.ByteBuffer;
public final class HudRegionSamplerSmokeTest {
 private static void check(boolean v,String label) {if(!v) throw new AssertionError(label);}
 public static void main(String[] ignored) {
  int w=160,h=90,stride=w*4+8;
  ByteBuffer b=ByteBuffer.allocate(stride*h);
  HudRegionSampler.Zones empty=HudRegionSampler.sample(b,w,h,stride,4);
  check(empty.mapMean==0&&empty.controlsMean==0,"black background");
  check(empty.mapContrast==0&&empty.controlsContrast==0,"no contrast in black");
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int i=y*stride+x*4;
   if(x>=w*.65&&y>=h*.55){b.put(i,(byte)180);b.put(i+1,(byte)180);b.put(i+2,(byte)180);}
  }
  HudRegionSampler.Zones lit=HudRegionSampler.sample(b,w,h,stride,4);
  check(lit.controlsMean>100,"bright controls zone");
  check(lit.mapMean==0,"map remains dark");
  check(lit.centerMean==0,"center remains dark");
  check(lit.width==w&&lit.height==h,"size retained");
  for(int y=0;y<h;y++)for(int x=(int)(w*.65);x<w;x++)if((x/8)%2==0){
   int i=y*stride+x*4;b.put(i,(byte)0);b.put(i+1,(byte)0);b.put(i+2,(byte)0);
  }
  HudRegionSampler.Zones mixed=HudRegionSampler.sample(b,w,h,stride,4);
  check(mixed.controlsContrast>0,"contrast detected");
  boolean failed=false;
  try{HudRegionSampler.sample(b,w,h,stride,3);}catch(IllegalArgumentException e){failed=true;}
  check(failed,"reject bad pixel stride");
  failed=false;
  try{HudRegionSampler.sample(ByteBuffer.allocate(20),w,h,stride,4);}catch(IllegalArgumentException e){failed=true;}
  check(failed,"reject short buffer");
  System.out.println("HudRegionSampler: 8 checks OK");
 }
}
