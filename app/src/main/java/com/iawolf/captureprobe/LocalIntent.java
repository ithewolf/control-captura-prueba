package com.iawolf.captureprobe;
import java.text.Normalizer;
import java.util.Locale;

/** Small deterministic Spanish intent router. Not a conversational LLM. */
public final class LocalIntent {
 private LocalIntent(){}
 private static boolean any(String s,String... terms){
  for(String term:terms)if(s.contains(term))return true;
  return false;
 }
 public static String reply(String heard,String mode,boolean fresh,boolean dark,boolean capturing){
  if(heard==null||heard.trim().isEmpty())return null;
  String s=Normalizer.normalize(heard,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
    .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]+"," ").replaceAll(" +"," ").trim();
  boolean companion="companero".equals(mode);
  if(any(s,"me escuch","escuchas","podes escuchar","estas ahi","me ois","me oyes"))
   return companion?"Sí, Wolf, te escucho. ¿De qué querés charlar?":"Sí, Wolf, te escucho. ¿Qué querés practicar?";
  if(any(s,"hola","buenas","buen dia","buenas noches"))
   return companion?"¡Buenas, Wolf! Acá estoy. ¿Cómo venís?":"Hola, Wolf. ¿Qué querés entrenar hoy?";
  if(any(s,"como estas","como andas","todo bien"))
   return companion?"Todo en orden por acá, Wolf. ¿Cómo va esa jornada?":"Listo para ayudarte a entrenar, Wolf. ¿Qué querés mejorar?";
  if(any(s,"que modo","modo actual","modo estas","sos companero","sos coach"))
   return companion?"Estoy en modo Compañero.":"Estoy en modo Coach.";
  if(any(s,"que ves","ves la pantalla","captura","pantalla","estas viendo"))
   return !capturing?"La captura no está activa.":
    !fresh?"Recibo la captura, pero no tengo una imagen reciente para interpretar.":
    dark?"La imagen reciente está oscura; no puedo identificar qué ocurre.":
    "Recibo imágenes de la pantalla, pero todavía no reconozco campeones ni decisiones tácticas.";
  if(any(s,"consejo","que hago","que deberia","estrategia","tactica","build","enemigo","jungla","objetivo","rotacion","pelea"))
   return "Todavía no tengo visión táctica entrenada. Decime el campeón y la situación, y podremos preparar una respuesta más específica cuando integremos esa función.";
  if(any(s,"quien sos","como te llamas"))
   return companion?"Soy CONTROL, tu compañero de voz.":"Soy CONTROL, tu asistente de entrenamiento.";
  if(any(s,"gracias","muchas gracias"))
   return companion?"¡De una, Wolf!":"De nada, Wolf.";
  if(any(s,"chau","adios","hasta luego"))
   return "Nos vemos, Wolf.";
  return "Te escuché, pero todavía no entiendo esa pregunta. Podés preguntarme si te escucho, qué modo uso o si recibo la pantalla.";
 }
}
