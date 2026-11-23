import com.iawolf.captureprobe.EnemyObservationGate;
import com.iawolf.captureprobe.EnemyPlanPipeline;
import com.iawolf.captureprobe.TacticalPlanCoordinator;

public final class EnemyPlanPipelineTest {
    static void yes(boolean value,String message){if(!value) throw new AssertionError(message);}
    static EnemyObservationGate.Detection seen(String id,String team,float conf,long at) {
        return new EnemyObservationGate.Detection(id,team,conf,.4f,.5f,at);
    }
    public static void main(String[] unused) {
        EnemyPlanPipeline pipeline=new EnemyPlanPipeline();
        var intent=TacticalPlanCoordinator.Intent.ATTACK;
        yes(!pipeline.ingest(seen("Warwick","ENEMY",.95f,1000),20,0,intent).actionable,"first observation cannot trigger");
        var danger=pipeline.ingest(seen("Warwick","ENEMY",.97f,1700),20,0,intent);
        yes(danger.actionable,"second consistent enemy detection can trigger tactics");
        yes(danger.primary==TacticalPlanCoordinator.Plan.DEFEND_AND_RETREAT,"retreat at low health");
        yes(danger.contingency==TacticalPlanCoordinator.Plan.DEFEND_AND_RETREAT,"safe contingency");
        pipeline.reset();
        yes(!pipeline.ingest(seen("Warwick","ENEMY",.90f,3000),70,0,intent).actionable,"reset clears per-match memory");
        yes(!pipeline.ingest(seen("Warwick","ALLY",.98f,3100),70,0,intent).actionable,"ally is not enemy");
        yes(!pipeline.ingest(seen("Warwick","ENEMY",.99f,3200),70,0,intent).actionable,"not after ally");
        yes(!pipeline.ingest(seen("LeeSin","ENEMY",.99f,3300),70,0,intent).actionable,"different identity");
        yes(!pipeline.ingest(seen("LeeSin","ENEMY",.70f,3400),70,0,intent).actionable,"low confidence");
        yes(!pipeline.ingest(seen("LeeSin","ENEMY",.98f,3500),70,0,intent).actionable,"low confidence resets state");
        yes(!pipeline.ingest(seen("LeeSin","ENEMY",.98f,7000),70,0,intent).actionable,"stale gap");
        yes(pipeline.ingest(seen("LeeSin","ENEMY",.98f,7100),70,0,intent).actionable,"confirmed after fresh matching frame");
        System.out.println("EnemyPlanPipeline: 12 checks OK");
    }
}
