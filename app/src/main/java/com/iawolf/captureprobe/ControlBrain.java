package com.iawolf.captureprobe;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Puente local de diagnóstico entre la voz del jugador y el estado de captura.
 * NO identifica campeones, enemigos ni interpreta tácticas de Wild Rift.
 */
public final class ControlBrain {
 private ControlBrain(){}

 public static String replyTo(String heard, boolean capturing, long frames, long changes) {
  if(heard==null || heard.trim().isEmpty())return null;
  String s=Normalizer.normalize(heard,Normalizer.Form.NFD)
   .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);
  // Palabra de activación para evitar comentar conversaciones ajenas.
  if(!s.contains("control"))return null;
  if(s.contains("estado") || s.contains("pantalla") || s.contains("captura") || s.contains("ves")){
   if(!capturing)return "Wolf, te escucho, pero la captura de pantalla no está activa.";
   if(frames==0)return "Wolf, te escucho. La captura está iniciada, pero todavía no recibí cuadros analizables.";
   return "Wolf, te escucho. La captura está activa y recibí " + frames +
    " cuadros muestreados. Todavía no interpreto lo que ocurre en el juego.";
  }
  if(s.contains("hola") || s.contains("escuch") || s.contains("prueba") || s.contains("habla")){
   return "Sí, Wolf, te escuché. Estoy recibiendo audio en el mismo servicio que captura la pantalla.";
  }
  return "Te escuché, Wolf. Mi conexión de audio está activa, pero mi cerebro táctico todavía está en construcción.";
 }
}
