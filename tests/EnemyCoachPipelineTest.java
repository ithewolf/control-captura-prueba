import com.iawolf.captureprobe.EnemyCoachPipeline;
import com.iawolf.captureprobe.EnemyObservationGate;
import com.iawolf.captureprobe.TacticalDecisionEngine;
public class EnemyCoachPipelineTest {
 static void chk(boolean x,String why){if(!x)throw new AssertionError(why);}
 static EnemyObservationGate.Detection d(String id,String side,long t){
   return new EnemyObservationGate.Detection(id,side,.93f,.4f,.6f,t);
 }
 public static void main(String[] args){
   EnemyCoachPipeline p=new EnemyCoachPipeline();
   chk(p.ingest(d("yasuo","ENEMY",1000),-1,-1).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"not first frame");
   chk(p.ingest(d("yasuo","ENEMY",1100),-1,-1).action==TacticalDecisionEngine.Action.IDENTIFY_ENEMY,"confirm without hp");
   p.reset();
   chk(p.ingest(d("fiora","ALLY",1200),90,1).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"ally");
   chk(p.ingest(d("fiora","ENEMY",1300),90,1).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"one frame");
   chk(p.ingest(d("fiora","ENEMY",1450),20,1).action==TacticalDecisionEngine.Action.RETREAT,"danger to player");
   System.out.println("EnemyCoachPipeline: 5/5 tests OK");
 }
}
