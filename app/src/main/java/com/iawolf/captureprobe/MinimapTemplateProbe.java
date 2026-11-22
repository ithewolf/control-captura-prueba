package com.iawolf.captureprobe;

/**
 * Experimental grayscale normalized cross-correlation on a cropped minimap.
 * It returns a visual template candidate ONLY, not a champion, ally or enemy.
 * Team/identity requires independently labeled game-specific templates and validation.
 * Pure Java: testable without Android/OpenCV.
 */
public final class MinimapTemplateProbe {
  private MinimapTemplateProbe(){}
  public static final class Match {
    public final boolean candidate;
    public final int x,y;
    public final double correlation;
    public final double contrast;
    Match(boolean candidate,int x,int y,double correlation,double contrast) {
      this.candidate=candidate;this.x=x;this.y=y;this.correlation=correlation;this.contrast=contrast;
    }
  }
  private static final double MIN_CORRELATION=0.93;
  private static final double MIN_CONTRAST=12.0;
  private static final int MAX_CANDIDATE_PIXELS=320*320;

  /**
   * pixels are row-major unsigned grayscale [0,255]. Coordinates are top-left of
   * matched template. No template supplied => no detection.
   */
  public static Match search(int[] pixels,int width,int height,int[] template,int tw,int th) {
    if(pixels==null||template==null||width<1||height<1||tw<3||th<3||
       tw>width||th>height||((long)width*height)>MAX_CANDIDATE_PIXELS||
       pixels.length!=(long)width*height||template.length!=(long)tw*th)
      throw new IllegalArgumentException("Bad minimap or template geometry");
    for(int v:pixels)if(v<0||v>255)throw new IllegalArgumentException("Bad pixel");
    for(int v:template)if(v<0||v>255)throw new IllegalArgumentException("Bad template pixel");
    int n=tw*th;
    double meanT=0;
    for(int t:template)meanT+=t;
    meanT/=n;
    double varianceT=0;
    for(int t:template){double d=t-meanT;varianceT+=d*d;}
    double contrastT=Math.sqrt(varianceT/n);
    if(contrastT<MIN_CONTRAST)return new Match(false,-1,-1,0,contrastT);
    double best=-2;int bx=-1,by=-1;
    for(int y=0;y<=height-th;y++)for(int x=0;x<=width-tw;x++){
      double meanP=0;
      for(int j=0;j<th;j++)for(int i=0;i<tw;i++)meanP+=pixels[(y+j)*width+x+i];
      meanP/=n;
      double dot=0,varP=0;
      for(int j=0;j<th;j++)for(int i=0;i<tw;i++){
        int k=j*tw+i;
        double a=template[k]-meanT, b=pixels[(y+j)*width+x+i]-meanP;
        dot+=a*b;varP+=b*b;
      }
      double corr=varP<1e-8?-1:dot/Math.sqrt(varianceT*varP);
      if(corr>best){best=corr;bx=x;by=y;}
    }
    return new Match(best>=MIN_CORRELATION,bx,by,best,contrastT);
  }
}
