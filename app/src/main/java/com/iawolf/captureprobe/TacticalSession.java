package com.iawolf.captureprobe;

/**
 * Memoria breve de una conversación táctica. Sin Android ni llamadas de red.
 * Acepta exclusivamente hechos declarados por el jugador, no infiere objetos de la pantalla.
 * El estado caduca para evitar consejos basados en información vieja.
 */
public final class TacticalSession {
  public static final long CONTEXT_TTL_MS = 90_000L;

  private boolean enemyKnown,enemyNear,allyKnown,allyNear,escapeKnown,escapeAvailable;
  private long enemyFactAt=-1L,allyFactAt=-1L,escapeFactAt=-1L;
  private String role="";
  private String lastTopic="";
  private long lastTopicAt=-1L;

  public synchronized void reset() {
    enemyKnown=allyKnown=escapeKnown=false;
    enemyNear=allyNear=escapeAvailable=false;
    enemyFactAt=allyFactAt=escapeFactAt=-1L;lastTopic="";lastTopicAt=-1L;role="";
  }

  public synchronized TacticalBrain.Decision respond(String utterance, boolean capturing, long frames, long nowMs) {
    if(nowMs<0)throw new IllegalArgumentException("nowMs must be nonnegative");
    String s=TacticalBrain.normalize(utterance);
    // Each fact expires independently: refreshing an ally must not revive an old enemy.
    if(expired(enemyFactAt,nowMs))enemyKnown=false;
    if(expired(allyFactAt,nowMs))allyKnown=false;
    if(expired(escapeFactAt,nowMs))escapeKnown=false;
    if(lastTopicAt>=0 && (nowMs<lastTopicAt || nowMs-lastTopicAt>CONTEXT_TTL_MS)) {
      lastTopic="";
    }
    // Explicit declarations. Do not interpret hypothetical questions as facts.
    boolean hypothetical=s.contains("si ") || s.startsWith("y si ") || s.contains("supon");
    boolean factChanged=false;
    if(!hypothetical) {
      if(contains(s,"no hay enemigos cerca","ningun enemigo cerca","no tengo enemigos cerca","enemigos lejos")) {
        enemyKnown=true;enemyNear=false;enemyFactAt=nowMs;factChanged=true;
      } else if(contains(s,"hay un enemigo cerca","hay enemigos cerca","enemigo cerca","enemigos cerca","me estan gankeando","me persiguen")) {
        enemyKnown=true;enemyNear=true;enemyFactAt=nowMs;factChanged=true;
      }
      if(contains(s,"estoy solo","sin aliados","no hay aliados cerca","mis aliados estan lejos")) {
        allyKnown=true;allyNear=false;allyFactAt=nowMs;factChanged=true;
      } else if(contains(s,"aliados cerca","mi equipo esta conmigo","tengo apoyo cerca","mis aliados estan conmigo")) {
        allyKnown=true;allyNear=true;allyFactAt=nowMs;factChanged=true;
      }
      if(contains(s,"no tengo escape","sin escape","no tengo flash","sin destello")) {
        escapeKnown=true;escapeAvailable=false;escapeFactAt=nowMs;factChanged=true;
      } else if(contains(s,"tengo flash","tengo destello","tengo escape")) {
        escapeKnown=true;escapeAvailable=true;escapeFactAt=nowMs;factChanged=true;
      }
      if(contains(s,"soy jungla","estoy jungla","juego jungla"))role="jungla";
      else if(contains(s,"soy soporte","juego soporte"))role="soporte";
      else if(contains(s,"soy adc","juego adc","soy tirador"))role="adc";
      else if(contains(s,"soy mid","juego mid","voy medio"))role="mid";
      else if(contains(s,"soy top","juego top","voy baron"))role="top";
    }

    TacticalBrain.Context.Builder b=new TacticalBrain.Context.Builder().capture(capturing,frames).role(role);
    if(enemyKnown)b.enemyNear(enemyNear);
    if(allyKnown)b.allyNear(allyNear);
    if(escapeKnown)b.escapeAvailable(escapeAvailable);
    TacticalBrain.Context ctx=b.build();

    TacticalBrain.Intent current=TacticalBrain.intent(s);
    boolean followup=contains(s,"y ahora","entonces","que hago","y despues","que recomendas","que me conviene");
    String question=s;
    if(current==TacticalBrain.Intent.UNKNOWN && !lastTopic.isEmpty() && (followup || factChanged)) {
      // When the player adds facts, reassess the active situation immediately.
      question=lastTopic;
    }
    if(current==TacticalBrain.Intent.UNKNOWN && factChanged && question.equals(s)) {
      return new TacticalBrain.Decision(TacticalBrain.Intent.UNKNOWN,
        "Entendido. Registré ese dato para esta situación. ¿Querés evaluar una pelea, un gank o un objetivo?",
        "dato del jugador almacenado; no supone detección visual",true);
    }
    TacticalBrain.Decision answer=TacticalBrain.decide(question,ctx);
    if(answer.intent!=TacticalBrain.Intent.UNKNOWN && answer.intent!=TacticalBrain.Intent.STATUS && answer.intent!=TacticalBrain.Intent.GREET && answer.intent!=TacticalBrain.Intent.ROLE) {
      lastTopic=question;
      lastTopicAt=nowMs;
    }
    return answer;
  }

  public synchronized String currentRole(){return role;}

  private static boolean expired(long at,long now){
    return at>=0 && (now<at || now-at>CONTEXT_TTL_MS);
  }

  private static boolean contains(String s,String... phrases) {
    for(String p:phrases)if(s.contains(p))return true;
    return false;
  }
}
