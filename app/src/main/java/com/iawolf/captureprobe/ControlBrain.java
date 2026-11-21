package com.iawolf.captureprobe;

/**
 * Puente de compatibilidad con VoiceBridge.
 * Las frases habladas requieren "CONTROL" para no interrumpir conversaciones ajenas.
 * Las pruebas de texto pueden invocar TacticalBrain.decide(...) directamente.
 */
public final class ControlBrain {
  private ControlBrain() {}
  public static String replyTo(String heard, boolean capturing, long frames, long changes) {
    String normalized=TacticalBrain.normalize(heard);
    if(normalized.isEmpty() || !normalized.matches(".*\\bcontrol\\b.*")) return null;
    // Nunca se atribuyen detecciones visuales a un simple contador de fotogramas.
    TacticalBrain.Context context=new TacticalBrain.Context.Builder().capture(capturing,frames).build();
    return TacticalBrain.decide(heard,context).answer;
  }
}
