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
    expect(d.intent==TacticalBrain.Intent.GANK,"revisar gank al recibir nuevo dato");
    expect(d.answer.contains("amenaza cerca"),"mantener advertencia al ampliar contexto");
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
    s.reset();
    d=s.respond("soy adc",true,0,600_000);
    expect(d.intent==TacticalBrain.Intent.ROLE,"declaración de rol no es objetivo");
    expect(d.answer.contains("adc"),"confirma rol declarado");
    d=s.respond("pelear",true,0,601_000);
    expect(d.answer.contains("Como tirador"),"rol persiste en conversación");
    s.reset();
    d=s.respond("enemigos cerca",true,0,700_000);
    d=s.respond("aliados cerca",true,0,780_000);
    d=s.respond("pelear",true,0,791_001);
    expect(d.needsMoreContext,"el hecho aliado no prolonga enemigo vencido");
    expect(!d.answer.contains("Hay aliados y rivales cerca"),"no aconsejar con enemigo expirado");
    s.reset();
    d=s.respond("enemigos cerca",true,0,800_000);
    d=s.respond("no hay enemigos cerca",true,0,800_001);
    d=s.respond("pelear",true,0,800_002);
    expect(d.needsMoreContext,"nueva declaración corrige enemigo anterior");
    s.reset();
    d=s.respond("soy jungla",true,0,900_000);
    expect("jungla".equals(s.currentRole()),"persistencia de rol jungla");
    d=s.respond("y ahora que hago",true,0,900_100);
    expect(d.intent==TacticalBrain.Intent.UNKNOWN,"declarar rol no inventa un tema anterior");
    s.reset();
    d=s.respond("tengo aliados cerca",true,10,950_000);
    expect(d.answer.contains("Registré ese dato"),"confirmar dato sin inventar una pelea");
    expect(d.needsMoreContext,"no atribuir decisión a dato aislado");
    s.reset();
    d=s.respond("me están gankeando",true,20,960_000);
    d=s.respond("estoy solo",true,22,960_200);
    expect(d.intent==TacticalBrain.Intent.GANK,"hecho nuevo revisa el gank activo");
    d=s.respond("y ahora que hago",true,22,960_300);
    expect(d.intent==TacticalBrain.Intent.GANK,"seguimiento conserva tema revisado");
    System.out.println("TacticalSessionTest: "+checks+" comprobaciones correctas");
  }
}
