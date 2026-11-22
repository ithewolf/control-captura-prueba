import com.iawolf.captureprobe.EnemyObservationGate;
public class EnemyObservationGateTest {
 static void eq(boolean value,String msg){if(!value)throw new AssertionError(msg);}
 static EnemyObservationGate.Detection d(String id,String side,float confidence,float x,float y,long ms){
   return new EnemyObservationGate.Detection(id,side,confidence,x,y,ms);
 }
 public static void main(String[] args){
   EnemyObservationGate gate=new EnemyObservationGate();
   eq(!gate.observe(d("yasuo","ENEMY",.92f,.5f,.5f,1000)).confirmed,"first frame");
   eq(gate.observe(d("yasuo","ENEMY",.92f,.51f,.52f,1500)).confirmed,"two frames");
   eq(!gate.observe(d("yasuo","ALLY",.99f,.52f,.52f,1800)).confirmed,"ally not enemy");
   eq(!gate.observe(d("yasuo","ENEMY",.95f,.5f,.5f,2100)).confirmed,"reset after ally");
   eq(!gate.observe(d("yasuo","ENEMY",.79f,.5f,.5f,2250)).confirmed,"low confidence");
   eq(!gate.observe(d("yasuo","ENEMY",.99f,.5f,.5f,2400)).confirmed,"after low confidence");
   eq(!gate.observe(d("yasuo","ENEMY",.99f,.9f,.9f,2500)).confirmed,"discontinuous position");
   eq(!gate.observe(d("fiora","ENEMY",.95f,.9f,.9f,2800)).confirmed,"different identity");
   eq(!gate.observe(d("fiora","UNKNOWN",.99f,.9f,.9f,3000)).confirmed,"unknown team");
   eq(!gate.observe(d("fiora","ENEMY",.99f,.9f,.9f,3200)).confirmed,"unknown reset");
   eq(!gate.observe(d("fiora","ENEMY",.99f,.9f,.9f,5000)).confirmed,"stale");
   eq(!gate.observe(null).confirmed,"null detection");
   eq(!gate.observe(d("warwick","ENEMY",Float.NaN,.3f,.4f,5500)).confirmed,"NaN confidence");
   eq(!gate.observe(d("warwick","ENEMY",.95f,-.3f,.4f,5600)).confirmed,"invalid position");
   eq(!gate.observe(d("warwick","ENEMY",.95f,.3f,.4f,5700)).confirmed,"new first frame");
   eq(gate.observe(d("warwick","ENEMY",.96f,.31f,.41f,5850)).confirmed,"warwick confirmed");
   System.out.println("EnemyObservationGate: 16/16 tests OK");
 }
}
