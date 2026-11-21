package com.iawolf.captureprobe;

import java.nio.ByteBuffer;

/**
 * Small CPU-only telemetry of normalized UI zones in an RGBA_8888 frame.
 * These are pixel statistics, NEVER game-state or tactical observations.
 * Geometry is normalized so portrait frames still produce statistics, but
 * the regions have only been designed for landscape game orientation.
 */
public final class HudRegionSampler {
 private HudRegionSampler() {}
 public static final class Zones {
  public final int width, height;
  public final int mapMean, controlsMean, centerMean;
  public final int mapContrast, controlsContrast;
  Zones(int w,int h,int m,int c,int mid,int mc,int cc) {
   width=w;height=h;mapMean=m;controlsMean=c;centerMean=mid;
   mapContrast=mc;controlsContrast=cc;
  }
 }
 private static int[] region(ByteBuffer buffer,int w,int h,int stride,int px,
                             double x0,double y0,double x1,double y1) {
  long sum=0, min=255, max=0;int count=0;
  for(int iy=0;iy<6;iy++)for(int ix=0;ix<8;ix++) {
   int x=Math.min(w-1,(int)((x0+(ix+.5)*(x1-x0)/8)*w));
   int y=Math.min(h-1,(int)((y0+(iy+.5)*(y1-y0)/6)*h));
   int i=y*stride+x*px;
   int r=buffer.get(i)&255,g=buffer.get(i+1)&255,b=buffer.get(i+2)&255;
   int v=(77*r+150*g+29*b)>>8;
   sum+=v;min=Math.min(min,v);max=Math.max(max,v);count++;
  }
  return new int[] {(int)(sum/count),(int)(max-min)};
 }
 public static Zones sample(ByteBuffer buffer,int w,int h,int stride,int px) {
  if(buffer==null||w<=0||h<=0||px<4||stride<(long)w*px)
   throw new IllegalArgumentException("bad RGBA geometry");
  long last=(long)(h-1)*stride+(long)(w-1)*px+2;
  if(last>=buffer.limit())throw new IllegalArgumentException("truncated RGBA buffer");
  // All zones are observational. None of these pixel readings means HUD present.
  int[] map=region(buffer,w,h,stride,px,0.00,0.00,0.30,0.35);
  int[] controls=region(buffer,w,h,stride,px,0.65,0.55,1.00,1.00);
  int[] center=region(buffer,w,h,stride,px,0.35,0.30,0.65,0.70);
  return new Zones(w,h,map[0],controls[0],center[0],map[1],controls[1]);
 }
}
