package com.iawolf.captureprobe;

/**
 * Adaptador de entrada conversacional. Funciona sin servicios de nube.
 * El motor de voz aporta texto; TacticalSession retiene únicamente hechos explícitos
 * durante un máximo de 90 segundos, sin imaginar información de la pantalla.
 */
public final class ControlBrain {
  private static final TacticalSession session=new TacticalSession();
  private ControlBrain() {}

  public static synchronized void resetSession() { session.reset(); }

  public static synchronized String replyTo(String heard, boolean capturing, long frames, long changes) {
    String normalized=TacticalBrain.normalize(heard);
    // La palabra de activación es obligatoria para la interfaz de voz.
    if(normalized.isEmpty() || !normalized.matches(".*\\bcontrol\\b.*"))return null;
    return session.respond(heard,capturing,frames,System.nanoTime()/1_000_000L).answer;
  }
}
