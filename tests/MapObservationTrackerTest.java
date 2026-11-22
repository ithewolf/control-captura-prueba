import com.iawolf.captureprobe.MapObservationTracker;
public final class MapObservationTrackerTest {
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args){
  MapObservationTracker t=new MapObservationTracker();
  check(t.state("enemy1",0).visibility==MapObservationTracker.Visibility.UNKNOWN,"unseen unknown");
  check(!t.observe("enemy1",MapObservationTracker.Team.ENEMY,0.2,0.3,0.4,1000),"reject weak detection");
  check(!t.observe("enemy1",MapObservationTracker.Team.ENEMY,-1,0.3,0.9,1000),"reject invalid coord");
  check(!t.observe("enemy1",MapObservationTracker.Team.ENEMY,Double.NaN,0.3,0.9,1000),"reject NaN");
  check(t.observe("enemy1",MapObservationTracker.Team.ENEMY,0.2,0.3,0.9,1000),"accept detection");
  check(t.state("enemy1",1100).visibility==MapObservationTracker.Visibility.VISIBLE,"fresh visible");
  check(t.state("enemy1",3000).visibility==MapObservationTracker.Visibility.LAST_SEEN,"must not claim still visible");
  check(t.state("enemy1",13001).visibility==MapObservationTracker.Visibility.UNKNOWN,"expired unknown");
  check(Double.isNaN(t.state("enemy1",13001).x),"expired coords not usable");
  check(!t.observe("enemy1",MapObservationTracker.Team.ENEMY,0.8,0.9,0.9,999),"reject out-of-order");
  check(!t.observe("enemy1",MapObservationTracker.Team.ALLY,0.8,0.9,0.9,1200),"reject team changes");
  check(t.observe("enemy1",MapObservationTracker.Team.ENEMY,0.8,0.9,0.9,1200),"accept update");
  check(t.trackCount()==1,"one track");
  t.clear();check(t.state("enemy1",1300).visibility==MapObservationTracker.Visibility.UNKNOWN,"match reset");
  System.out.println("MAP_TRACKER_TEST_OK");
 }
}
