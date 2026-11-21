package com.iawolf.captureprobe;

/** Pruebas de memoria sin dependencias Android. */
public final class TacticalSessionTest {
  private static int checks=0;
  private static void expect(boolean yes,String label) {
    if(!yes)throw new AssertionError(label);
    checks++;
  }
  public static void main(String[] args) {
    TacticalSession s=new TacticalSession();
    TacticalBrain.Decision d=s.respond("me están gankeando",true,22,1000);
    expect(d.intent==TacticalBrain.Intent.GANK,"detectar gank");
    d=s.respond("estoy solo",true,25,2000);
    expect(d.intent==TacticalBrain.Intent.UNKNOWN,"declaración no supone una pregunta");
    d=s.respond("y ahora qué hago",true,30,3000);
    expect(d.intent==TacticalBrain.Intent.GANK,"recordar tema anterior");
    expect(d.answer.contains("amenaza cerca"),"conservar enemigo próximo");
    d=s.respond("y ahora qué hago",true,31,94_001);
    expect(d.needsMoreContext,"caduca información táctica obsoleta");
    expect(d.intent==TacticalBrain.Intent.UNKNOWN,"también caduca tema anterior");
    s.reset();
    d=s.respond("hay enemigos cerca y tengo apoyo cerca",true,12,100_000);
    d=s.respond("conviene pelear",true,13,100_100);
    expect(d.answer.contains("aliados y rivales"),"combinar hechos declarados");
    s.reset();
    d=s.respond("no hay enemigos cerca",false,0,200_000);
    d=s.respond("dragón",false,0,200_001);
    expect(d.needsMoreContext,"no inventar apoyo aliado");
    s.reset();
    d=s.respond("si hay enemigos cerca",false,0,300_000);
    d=s.respond("dragón",false,0,300_001);
    expect(d.needsMoreContext,"suposición no se vuelve detección");
    s.reset();
    d=s.respond("soy jungla",false,0,400_000);
    expect("jungla".equals(s.currentRole()),"rol explícito");
    s.reset();
    expect("".equals(s.currentRole()),"reiniciar rol");
    d=s.respond("Control, ¿y ahora peleo?",false,0,500_000);
    expect(d.intent==TacticalBrain.Intent.FIGHT,"lenguaje natural peleo");
    expect(d.needsMoreContext,"pelea sin visión ni hechos requiere contexto");
    System.out.println("TacticalSessionTest: "+checks+" comprobaciones correctas");
  }
}
