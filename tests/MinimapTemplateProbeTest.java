import com.iawolf.captureprobe.MinimapTemplateProbe;
import java.util.Arrays;
public final class MinimapTemplateProbeTest {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args) {
  int w=32,h=24,tw=5,th=5;
  int[] template=new int[tw*th];
  for(int y=0;y<th;y++)for(int x=0;x<tw;x++)template[y*tw+x]=(x*41+y*53+x*y*9)%255;
  int[] image=new int[w*h];
  Arrays.fill(image,30);
  int ox=17,oy=9;
  for(int y=0;y<th;y++)for(int x=0;x<tw;x++)image[(oy+y)*w+ox+x]=template[y*tw+x];
  var found=MinimapTemplateProbe.search(image,w,h,template,tw,th);
  ok(found.candidate,"known patch");
  ok(found.x==ox&&found.y==oy,"position matches");
  ok(found.correlation>0.999,"perfect match score");
  Arrays.fill(image,30);
  var missing=MinimapTemplateProbe.search(image,w,h,template,tw,th);
  ok(!missing.candidate,"uniform image is not a template match");
  int[] flat=new int[25];Arrays.fill(flat,128);
  ok(!MinimapTemplateProbe.search(image,w,h,flat,tw,th).candidate,"flat template must be rejected");
  boolean error=false;
  try{MinimapTemplateProbe.search(image,w,h,template,0,th);}catch(IllegalArgumentException e){error=true;}
  ok(error,"reject invalid geometry");
  image[0]=300; error=false;
  try{MinimapTemplateProbe.search(image,w,h,template,tw,th);}catch(IllegalArgumentException e){error=true;}
  ok(error,"reject invalid pixels");
  System.out.println("MINIMAP_TEMPLATE_PROBE_TEST_OK");
 }
}
