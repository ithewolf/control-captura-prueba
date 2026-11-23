package com.iawolf.captureprobe;
import java.util.*;
/** 100 tactical MULTIPLE-CHOICE scenarios; fixed choices are the five Action enum values.
 * Exposes limitations: these are deterministic rule-policy cases, NOT free-form reasoning.
 */
public final class Exam100 {
  static int total=0,correct=0;
  static Map<String,Integer> fail=new TreeMap<>();
  static void question(String topic,int hp,int enemies,int allies,boolean reliable,boolean fresh,boolean wave,boolean confirmed,boolean abilities,String expected){
    int q=++total;
    TacticalDecisionEngine.State s=new TacticalDecisionEngine.State(hp,enemies,allies,reliable,fresh,wave,confirmed,abilities);
    String actual=TacticalDecisionEngine.evaluate(s).action.name();
    if(expected.equals(actual))correct++; else fail.merge(topic,1,Integer::sum);
    System.out.println("{\"number\":"+q+",\"topic\":\""+topic+"\",\"hp\":"+hp+",\"enemies\":"+enemies+",\"allies\":"+allies+",\"reliable\":"+reliable+",\"fresh\":"+fresh+",\"wave\":"+wave+",\"confirmed\":"+confirmed+",\"abilities\":"+abilities+",\"correct\":\""+expected+"\",\"answer\":\""+actual+"\",\"passed\":"+expected.equals(actual)+"}");
  }
  public static void main(String[] args){
    for(int i=0;i<10;i++)question("stale_or_unreliable",70,1,1,i%2==0,i%2!=0,true,true,true,"INSUFFICIENT_EVIDENCE");
    for(int i=0;i<10;i++)question("invalid_health", i%2==0?-2:101+i,1,1,true,true,true,true,true,"INSUFFICIENT_EVIDENCE");
    for(int i=0;i<10;i++)question("enemy_count_contradiction",50+i,0,1,true,true,true,true,true,"INSUFFICIENT_EVIDENCE");
    for(int i=0;i<10;i++)question("low_hp_with_enemy",20+i,1+i%3,2,true,true,true,true,i%2==0,"RETREAT");
    for(int i=0;i<10;i++)question("outnumbered_enemy",70,3+i%3,0+i%2,true,true,false,true,i%2==0,"RETREAT");
    for(int i=0;i<10;i++)question("enemy_visible_no_attack_assumption",65+i,1,1,true,true,false,true,i%2==0,"IDENTIFY_ENEMY");
    for(int i=0;i<10;i++)question("critical_hp_without_confirmed_enemy",12+i,0,1,true,true,false,false,false,"RECOVER");
    for(int i=0;i<10;i++)question("safe_farm_local_area",50+i*4,0,1,true,true,true,false,i%2==0,"FARM_SAFELY");
    for(int i=0;i<10;i++)question("unknown_enemy_count_not_zero",50+i,-1,1,true,true,true,false,i%2==0,"INSUFFICIENT_EVIDENCE");
    for(int i=0;i<10;i++)question("unknown_hp_with_enemy",-1,1,0,true,true,false,true,false,"IDENTIFY_ENEMY");
    System.err.println("SCORE "+correct+"/"+total+" pass="+(correct>=80)+" failures="+fail);
    if(total!=100 || correct<80)System.exit(1);
  }
}
