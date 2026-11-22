import com.iawolf.captureprobe.MinimapCoordinateNormalizer;
public final class MinimapCoordinateNormalizerTest {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  var normal=MinimapCoordinateNormalizer.normalize(.2,.8,MinimapCoordinateNormalizer.Orientation.NORMAL);
  check(normal!=null && Math.abs(normal.x-.2)<1e-9 && Math.abs(normal.y-.8)<1e-9,"normal");
  var rotated=MinimapCoordinateNormalizer.normalize(.2,.8,MinimapCoordinateNormalizer.Orientation.ROTATED_180);
  check(rotated!=null && Math.abs(rotated.x-.8)<1e-9 && Math.abs(rotated.y-.2)<1e-9,"rotation");
  check(MinimapCoordinateNormalizer.normalize(.2,.8,MinimapCoordinateNormalizer.Orientation.UNKNOWN)==null,"unknown orientation");
  check(MinimapCoordinateNormalizer.normalize(.2,.8,null)==null,"missing orientation");
  check(MinimapCoordinateNormalizer.normalize(Double.NaN,.2,MinimapCoordinateNormalizer.Orientation.NORMAL)==null,"NaN");
  check(MinimapCoordinateNormalizer.normalize(1.1,.2,MinimapCoordinateNormalizer.Orientation.NORMAL)==null,"out of bounds");
  System.out.println("MINIMAP_NORMALIZER_TEST_OK");
 }
}
