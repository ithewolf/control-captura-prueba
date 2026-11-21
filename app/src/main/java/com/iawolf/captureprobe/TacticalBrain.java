package com.iawolf.captureprobe;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Núcleo táctico v1. Java puro, sin Android, red, audio ni dependencias pesadas.
 * Los hechos del juego son EXCLUSIVAMENTE los aportados por el usuario o un detector validado.
 * No simula visión, mapas, porcentajes de vida ni un modelo lingüístico.
 */
public final class TacticalBrain {
  public enum Side { ALLY, ENEMY, NEUTRAL, UNKNOWN }
  public enum Intent { GREET, STATUS, GANK, OBJECTIVE, FIGHT, ROTATE, VISION, UNKNOWN }

  public static final class Context {
    public final boolean capturing;
    public final long frames;
    public final boolean enemyNear, allyNear, escapeAvailable, enemyVisible;
    public final boolean enemyNearKnown, allyNearKnown, escapeKnown, enemyVisibleKnown;
    public final String role;
    private Context(Builder b) {
      capturing=b.capturing; frames=b.frames; enemyNear=b.enemyNear; allyNear=b.allyNear;
      escapeAvailable=b.escapeAvailable; enemyVisible=b.enemyVisible;
      enemyNearKnown=b.enemyNearKnown; allyNearKnown=b.allyNearKnown;
      escapeKnown=b.escapeKnown; enemyVisibleKnown=b.enemyVisibleKnown;
      role=b.role;
    }
    public static final class Builder {
      private boolean capturing, enemyNear, allyNear, escapeAvailable, enemyVisible;
      private boolean enemyNearKnown, allyNearKnown, escapeKnown, enemyVisibleKnown;
      private long frames;
      private String role="";
      public Builder capture(boolean active,long n){capturing=active;frames=Math.max(0,n);return this;}
      public Builder enemyNear(boolean v){enemyNear=v;enemyNearKnown=true;return this;}
      public Builder allyNear(boolean v){allyNear=v;allyNearKnown=true;return this;}
      public Builder escapeAvailable(boolean v){escapeAvailable=v;escapeKnown=true;return this;}
      public Builder enemyVisible(boolean v){enemyVisible=v;enemyVisibleKnown=true;return this;}
      public Builder role(String v){role=v==null?"":v;return this;}
      public Context build(){return new Context(this);}
    }
  }

  public static final class Decision {
    public final Intent intent;
    public final String answer;
    public final String rationale;
    public final boolean needsMoreContext;
    Decision(Intent i,String a,String r,boolean need){intent=i;answer=a;rationale=r;needsMoreContext=need;}
  }

  private TacticalBrain(){}

  public static String normalize(String raw) {
    if(raw==null)return "";
    return Normalizer.normalize(raw,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
      .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
  }
  private static boolean any(String s,String... needles) {
    for(String needle:needles)if(s.contains(needle))return true;
    return false;
  }
  public static Side classify(String label) {
    String s=normalize(label);
    if(any(s,"aliado","aliada","amigo","nuestro equipo"))return Side.ALLY;
    if(any(s,"enemigo","enemiga","rival","oponente"))return Side.ENEMY;
    if(any(s,"neutral","monstruo","campamento","creep","minion","subdito","torre","dragon","baron","heraldo"))return Side.NEUTRAL;
    return Side.UNKNOWN;
  }

  public static Intent intent(String input) {
    String s=normalize(input);
    if(s.isEmpty())return Intent.UNKNOWN;
    if(any(s,"gank","gankear","embosc","me vienen","me persig","me atacan"))return Intent.GANK;
    if(any(s,"dragon","baron","heraldo","objetivo","jungla","smite"))return Intent.OBJECTIVE;
    if(any(s,"pelea","pelear","peleo","peleamos","teamfight","luchar","entrar","iniciar","retroceder","retir"))return Intent.FIGHT;
    if(any(s,"rotar","rotacion","torre destruida","tiramos torre","destruir torre","pushear","push"))return Intent.ROTATE;
    if(any(s,"cuantos enemigos","cuantos rivales","a quien ves","que ves","ves enemigos"))return Intent.VISION;
    if(any(s,"estado","captura","pantalla","funciona"))return Intent.STATUS;
    if(any(s,"hola","buenas","escuchas","prueba"))return Intent.GREET;
    return Intent.UNKNOWN;
  }
  public static Decision decide(String userText,Context context){
    Context c=context==null?new Context.Builder().build():context;
    String s=normalize(userText);
    Intent intent=intent(s);
    switch(intent) {
      case GREET:
        return new Decision(intent,"Acá estoy, Wolf. Decime qué situación querés analizar.","saludo",false);
      case STATUS:
        return new Decision(intent,!c.capturing?"La captura no está activa. Podemos practicar estrategia por texto.":
          c.frames==0?"La captura está iniciada, pero todavía no recibí imágenes analizables.":
          "La captura está activa y registró fotogramas. Eso no significa que ya reconozca campeones.",
          "diagnóstico sin inferencias visuales",false);
      case GANK:
        if(c.enemyNearKnown && c.enemyNear) {
          return new Decision(intent,c.escapeKnown&&!c.escapeAvailable?
            "Hay una amenaza cerca y no tengo confirmada una salida segura. Priorizá cobertura y ayuda aliada; evitá comprometerte más.":
            "Hay una amenaza cerca. Retrocedé hacia una zona segura, mirá el minimapa y guardá una vía de escape.",
            "priorizar supervivencia ante amenaza informada",false);
        }
        return new Decision(intent,"Si sospechás un gank, evitá adelantarte, revisá el minimapa y buscá una ruta de salida. ¿Tenés un enemigo cerca?",
          "no se confirmó un enemigo próximo",true);
      case OBJECTIVE:
        if(c.enemyNearKnown && c.enemyNear && c.allyNearKnown && !c.allyNear)
          return new Decision(intent,"No forzaría ese objetivo solo con un enemigo cerca. Reagrupate y buscá visión antes de entrar.",
            "riesgo por enemigo cercano sin aliados próximos",false);
        if(c.allyNearKnown && c.allyNear && c.enemyNearKnown && !c.enemyNear)
          return new Decision(intent,"Tenés apoyo cerca y no reportaste enemigos próximos. Prepará visión y verificá prioridades antes de intentar el objetivo.",
            "ventana posible pero no garantiza seguridad",false);
        return new Decision(intent,"Antes de disputar el objetivo, confirmá aliados cercanos, enemigos visibles, visión y rutas de retirada. ¿Tenés apoyo?",
          "información insuficiente para recomendar forzar",true);
      case FIGHT:
        if(c.enemyNearKnown && c.enemyNear && c.allyNearKnown && !c.allyNear)
          return new Decision(intent,"No iniciaría una pelea sin apoyo contra una amenaza cercana. Tomá distancia y pedí ayuda.",
            "riesgo de pelea sin apoyo",false);
        if(c.allyNearKnown && c.allyNear && c.enemyNearKnown && c.enemyNear)
          return new Decision(intent,"Hay aliados y rivales cerca. Coordiná habilidades, cuidá tu posición y evaluá antes de entrar; no puedo asegurar ventaja.",
            "encuentro con ambos equipos, sin datos suficientes para afirmar ventaja",false);
        return new Decision(intent,"No puedo recomendar entrar sin conocer cuántos aliados y enemigos están cerca. Contame la posición de tu equipo.",
          "contexto de pelea incompleto",true);
      case ROTATE:
        return new Decision(intent,"Después de una torre, mirá qué oleada podés empujar sin exponerte, controlá visión y considerá rotar hacia un objetivo con tu equipo.",
          "principios generales de rotación; no hay mapa confirmado",false);
      case VISION:
        return new Decision(intent,"Todavía no tengo un detector de unidades validado conectado al cerebro, así que no puedo decir cuántos enemigos veo.",
          "sin detecciones validadas",true);
      default:
        return new Decision(intent,"Entendí que querés ayuda, pero necesito saber si hablás de una pelea, un gank, un objetivo o una rotación.",
          "intención no identificada",true);
    }
  }
}
