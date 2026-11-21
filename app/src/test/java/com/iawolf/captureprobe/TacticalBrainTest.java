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
    ok(TacticalBrain.intent("Soy jungla")==TacticalBrain.Intent.ROLE,"rol no es objetivo");
    ok(TacticalBrain.intent("¿Cuántos enemigos ves en el dragón?")==TacticalBrain.Intent.VISION,"visión gana prioridad");
    ok(TacticalBrain.intent("voy por el Barón")==TacticalBrain.Intent.OBJECTIVE,"Barón como objetivo");
    ok(TacticalBrain.intent("farmear jungla")==TacticalBrain.Intent.OBJECTIVE,"farmear jungla");
    d=TacticalBrain.decide("soy soporte",new TacticalBrain.Context.Builder().role("soporte").build());
    ok(d.intent==TacticalBrain.Intent.ROLE && d.answer.contains("soporte"),"reconocer rol soporte");
    d=TacticalBrain.decide("pelear",new TacticalBrain.Context.Builder().role("adc").build());
    ok(d.answer.contains("Como tirador") && d.needsMoreContext,"pelea: consejo adc pero sin inventar ventaja");
    d=TacticalBrain.decide("dragón",new TacticalBrain.Context.Builder().role("jungla").build());
    ok(d.answer.contains("Castigo") && d.needsMoreContext,"objetivo jungla sin datos suficientes");
    d=TacticalBrain.decide("tiramos torre",new TacticalBrain.Context.Builder().role("mid").build());
    ok(d.answer.contains("Como mid"),"rotación específica para mid");
    d=TacticalBrain.decide("pelear",new TacticalBrain.Context.Builder().role("soporte").enemyNear(true).allyNear(false).build());
    ok(d.answer.contains("No iniciaría") && d.answer.contains("Como soporte"),"rol no anula prudencia");
    d=TacticalBrain.decide("dragón",new TacticalBrain.Context.Builder().role("top").build());
    ok(d.answer.contains("Como top"),"objetivo top");
    System.out.println("TacticalBrainTest: "+passed+" pruebas correctas");
  }
}
