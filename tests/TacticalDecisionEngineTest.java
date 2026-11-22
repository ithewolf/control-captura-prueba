import com.iawolf.captureprobe.TacticalDecisionEngine;
public class TacticalDecisionEngineTest {
  static void check(boolean x,String why) {if(!x)throw new AssertionError(why);}
  static TacticalDecisionEngine.State s(int hp,int en,int al,int eh,boolean reliable,boolean fresh,boolean wave,boolean champion,boolean abilities){
    return new TacticalDecisionEngine.State(hp,en,al,eh,reliable,fresh,wave,champion,abilities);
  }
  public static void main(String[] ignored){
    var e=TacticalDecisionEngine.Action.class;
    check(TacticalDecisionEngine.evaluate(null).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"null");
    check(TacticalDecisionEngine.evaluate(s(20,2,0,80,false,true,false,true,true)).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"unreliable");
    check(TacticalDecisionEngine.evaluate(s(20,2,0,80,true,false,false,true,true)).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"stale");
    check(TacticalDecisionEngine.evaluate(s(-1,2,0,80,true,true,false,true,true)).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"unknown hp");
    check(TacticalDecisionEngine.evaluate(s(20,-1,0,80,true,true,false,true,true)).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"unknown enemies");
    check(TacticalDecisionEngine.evaluate(s(28,2,0,80,true,true,false,true,true)).action==TacticalDecisionEngine.Action.RETREAT,"low hp");
    check(TacticalDecisionEngine.evaluate(s(20,0,0,80,true,true,false,false,false)).action==TacticalDecisionEngine.Action.RECOVER,"critical");
    check(TacticalDecisionEngine.evaluate(s(88,3,0,80,true,true,false,true,true)).action==TacticalDecisionEngine.Action.RETREAT,"outnumbered");
    check(TacticalDecisionEngine.evaluate(s(85,1,0,20,true,true,false,true,true)).action==TacticalDecisionEngine.Action.CONSIDER_ATTACK,"opportunity");
    check(TacticalDecisionEngine.evaluate(s(85,1,0,20,true,true,false,false,true)).action==TacticalDecisionEngine.Action.INSUFFICIENT_EVIDENCE,"champion unconfirmed");
    check(TacticalDecisionEngine.evaluate(s(85,0,0,-1,true,true,true,false,false)).action==TacticalDecisionEngine.Action.FARM_SAFELY,"farm");
    check(!TacticalDecisionEngine.evaluate(s(85,0,0,-1,true,false,true,false,false)).sufficientEvidence,"stale wave");
    System.out.println("TacticalDecisionEngine: 12/12 tests OK");
  }
}
