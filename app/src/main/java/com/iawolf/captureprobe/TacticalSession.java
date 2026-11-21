package com.iawolf.captureprobe;

/**
 * Memoria breve de una conversación táctica. Sin Android ni llamadas de red.
 * Acepta exclusivamente hechos declarados por el jugador, no infiere objetos de la pantalla.
 * El estado caduca para evitar consejos basados en información vieja.
 */
public final class TacticalSession {
  public static final long CONTEXT_TTL_MS = 90_000L;

  private boolean enemyKnown,enemyNear,allyKnown,allyNear,escapeKnown,escapeAvailable;
  private long lastFactAt=-1L;
  private String role="";
  private String lastTopic="";
  private long lastTopicAt=-1L;

  public synchronized void reset() {
    enemyKnown=allyKnown=escapeKnown=false;
    enemyNear=allyNear=escapeAvailable=false;
    lastFactAt=-1L;lastTopic="";lastTopicAt=-1L;role="";
  }

  public synchronized TacticalBrain.Decision respond(String utterance, boolean capturing, long frames, long nowMs) {
    if(nowMs<0)throw new IllegalArgumentException("nowMs must be nonnegative");
    String s=TacticalBrain.normalize(utterance);
    if(lastFactAt>=0 && (nowMs<lastFactAt || nowMs-lastFactAt>CONTEXT_TTL_MS)) {
      enemyKnown=allyKnown=escapeKnown=false;
    }
    if(lastTopicAt>=0 && (nowMs<lastTopicAt || nowMs-lastTopicAt>CONTEXT_TTL_MS)) {
      lastTopic="";
    }
    // Explicit declarations. Do not interpret hypothetical questions as facts.
    boolean hypothetical=s.contains("si ") || s.startsWith("y si ") || s.contains("supon");
    boolean updated=false;
    if(!hypothetical) {
      if(contains(s,"no hay enemigos cerca","ningun enemigo cerca","no tengo enemigos cerca","enemigos lejos")) {
        enemyKnown=true;enemyNear=false;updated=true;
      } else if(contains(s,"hay un enemigo cerca","hay enemigos cerca","enemigo cerca","enemigos cerca","me estan gankeando","me persiguen")) {
        enemyKnown=true;enemyNear=true;updated=true;
      }
      if(contains(s,"estoy solo","sin aliados","no hay aliados cerca","mis aliados estan lejos")) {
        allyKnown=true;allyNear=false;updated=true;
      } else if(contains(s,"aliados cerca","mi equipo esta conmigo","tengo apoyo cerca","mis aliados estan conmigo")) {
        allyKnown=true;allyNear=true;updated=true;
      }
      if(contains(s,"no tengo escape","sin escape","no tengo flash","sin destello")) {
        escapeKnown=true;escapeAvailable=false;updated=true;
      } else if(contains(s,"tengo flash","tengo destello","tengo escape")) {
        escapeKnown=true;escapeAvailable=true;updated=true;
      }
      if(contains(s,"soy jungla","estoy jungla","juego jungla"))role="jungla";
      else if(contains(s,"soy soporte","juego soporte"))role="soporte";
      else if(contains(s,"soy adc","juego adc","soy tirador"))role="adc";
      else if(contains(s,"soy mid","juego mid","voy medio"))role="mid";
      else if(contains(s,"soy top","juego top","voy baron"))role="top";
    }
    if(updated)lastFactAt=nowMs;

    TacticalBrain.Context.Builder b=new TacticalBrain.Context.Builder().capture(capturing,frames).role(role);
    if(enemyKnown)b.enemyNear(enemyNear);
    if(allyKnown)b.allyNear(allyNear);
    if(escapeKnown)b.escapeAvailable(escapeAvailable);
    TacticalBrain.Context ctx=b.build();

    TacticalBrain.Intent current=TacticalBrain.intent(s);
    boolean followup=contains(s,"y ahora","entonces","que hago","y despues","que recomendas","que me conviene");
    String question=s;
    if(current==TacticalBrain.Intent.UNKNOWN && followup && !lastTopic.isEmpty()) {
      question=lastTopic;
    }
    TacticalBrain.Decision answer=TacticalBrain.decide(question,ctx);
    if(answer.intent!=TacticalBrain.Intent.UNKNOWN && answer.intent!=TacticalBrain.Intent.STATUS && answer.intent!=TacticalBrain.Intent.GREET) {
      lastTopic=question;
      lastTopicAt=nowMs;
    }
    return answer;
  }

  public synchronized String currentRole(){return role;}

  private static boolean contains(String s,String... phrases) {
    for(String p:phrases)if(s.contains(p))return true;
    return false;
  }
}
