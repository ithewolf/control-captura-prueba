import com.iawolf.captureprobe.TacticalDecisionEngine;
import com.iawolf.captureprobe.TacticalPlanCoordinator;

public final class TacticalPlanCoordinatorTest {
    private static void yes(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    private static TacticalDecisionEngine.State state(int hp,int enemies,int allies,boolean reliable,boolean fresh,boolean wave,boolean champion) {
        return new TacticalDecisionEngine.State(hp,enemies,allies,reliable,fresh,wave,champion,false);
    }
    private static TacticalPlanCoordinator.Result run(TacticalDecisionEngine.State s,TacticalPlanCoordinator.Intent intent,boolean ally,boolean escape) {
        return TacticalPlanCoordinator.plan(new TacticalPlanCoordinator.Context(s,intent,ally,escape,false));
    }
    public static void main(String[] args) {
        var P=TacticalPlanCoordinator.Plan.class;
        yes(!TacticalPlanCoordinator.plan(null).actionable,"null");
        yes(!run(state(30,1,0,false,true,false,true),TacticalPlanCoordinator.Intent.UNKNOWN,false,true).actionable,"unreliable");
        yes(!run(state(30,1,0,true,false,false,true),TacticalPlanCoordinator.Intent.UNKNOWN,false,true).actionable,"stale");
        yes(!run(state(80,-1,-1,true,true,false,false),TacticalPlanCoordinator.Intent.UNKNOWN,false,true).actionable,"unknown is not safe");
        var dangerous=run(state(20,2,0,true,true,false,true),TacticalPlanCoordinator.Intent.ATTACK,false,true);
        yes(dangerous.actionable,"danger");
        yes(dangerous.primary==TacticalPlanCoordinator.Plan.DEFEND_AND_RETREAT,"protect player");
        yes(dangerous.contingency==TacticalPlanCoordinator.Plan.DEFEND_AND_RETREAT,"escape if attack fails");
        var enemy=run(state(80,1,1,true,true,false,true),TacticalPlanCoordinator.Intent.ATTACK,false,true);
        yes(enemy.primary!=TacticalPlanCoordinator.Plan.ATTACK_WITH_ESCAPE,"no attack without ability evidence");
        var ally=run(state(80,0,1,true,true,true,false),TacticalPlanCoordinator.Intent.UNKNOWN,true,true);
        yes(ally.primary==TacticalPlanCoordinator.Plan.SUPPORT_RETREAT,"ally support");
        var noEscape=run(state(80,0,1,true,true,true,false),TacticalPlanCoordinator.Intent.UNKNOWN,true,false);
        yes(noEscape.primary==TacticalPlanCoordinator.Plan.OBSERVE_AND_FARM,"no rescue without known escape");
        var farm=run(state(80,0,0,true,true,true,false),TacticalPlanCoordinator.Intent.FARM,false,false);
        yes(farm.primary==TacticalPlanCoordinator.Plan.OBSERVE_AND_FARM,"farm");
        yes(farm.contingency==TacticalPlanCoordinator.Plan.DEFEND_AND_RETREAT,"contingency");
        System.out.println("TacticalPlanCoordinator: 11/11 checks OK");
    }
}
