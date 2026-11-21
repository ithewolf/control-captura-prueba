package com.iawolf.captureprobe;

/** Pruebas Java puras: ejecutar javac y java -ea, sin Android ni micrófono. */
public final class TacticalBrainTest {
  private static int passed=0;
  private static void ok(boolean condition,String name){
    if(!condition)throw new AssertionError(name);
    passed++;
  }
  public static void main(String[] args){
    TacticalBrain.Context empty=new TacticalBrain.Context.Builder().build();
    ok(TacticalBrain.normalize("¡GANKÉAME!").equals("gankeame"),"normalización de acentos");
    ok(TacticalBrain.intent("Me están gankeando") == TacticalBrain.Intent.GANK,"gank escrito");
    ok(TacticalBrain.intent("Control, ¿conviene pelear?")==TacticalBrain.Intent.FIGHT,"pelea");
    ok(TacticalBrain.intent("Vamos por el dragón")==TacticalBrain.Intent.OBJECTIVE,"dragón");
    ok(TacticalBrain.intent("¿Qué hago después de destruir torre?")==TacticalBrain.Intent.ROTATE,"rotación");
    ok(TacticalBrain.classify("un enemigo")==TacticalBrain.Side.ENEMY,"enemigos");
    ok(TacticalBrain.classify("nuestro aliado")==TacticalBrain.Side.ALLY,"aliados");
    ok(TacticalBrain.classify("monstruo de jungla")==TacticalBrain.Side.NEUTRAL,"neutral");
    ok(TacticalBrain.classify("algo raro")==TacticalBrain.Side.UNKNOWN,"desconocidos");
    TacticalBrain.Decision d=TacticalBrain.decide("me están gankeando",empty);
    ok(d.needsMoreContext,"gank sin contexto debe pedir datos");
    ok(d.answer.contains("¿Tenés un enemigo cerca?"),"pregunta explícita");
    d=TacticalBrain.decide("pelear",new TacticalBrain.Context.Builder().enemyNear(true).allyNear(false).build());
    ok(d.answer.contains("No iniciaría"),"no aconsejar pelear solo");
    d=TacticalBrain.decide("dragon",new TacticalBrain.Context.Builder().enemyNear(true).allyNear(false).build());
    ok(d.answer.contains("No forzaría"),"no forzar objetivo solo");
    d=TacticalBrain.decide("dragon",new TacticalBrain.Context.Builder().enemyNear(false).allyNear(true).build());
    ok(d.answer.contains("verificá"),"no garantizar objetivo");
    d=TacticalBrain.decide("cuantos enemigos ves",empty);
    ok(d.needsMoreContext && d.answer.contains("no puedo decir"),"no inventar detecciones");
    d=TacticalBrain.decide("estado",new TacticalBrain.Context.Builder().capture(true,100).build());
    ok(d.answer.contains("no significa"),"captura no implica visión");
    d=TacticalBrain.decide("necesito ayuda con esto",empty);
    ok(d.needsMoreContext,"intención desconocida");
    d=TacticalBrain.decide(null,empty);
    ok(d.needsMoreContext,"texto nulo");
    d=TacticalBrain.decide("me persiguen",new TacticalBrain.Context.Builder().enemyNear(true).escapeAvailable(false).build());
    ok(d.answer.contains("cobertura"),"escape no disponible");
    System.out.println("TacticalBrainTest: "+passed+" pruebas correctas");
  }
}
