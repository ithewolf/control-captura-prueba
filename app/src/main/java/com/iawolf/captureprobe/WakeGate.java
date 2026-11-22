package com.iawolf.captureprobe;
import java.text.Normalizer;
import java.util.Locale;

/** Local, conservative wake-word gate for Vosk text. Not speaker recognition. */
public final class WakeGate {
 private WakeGate(){}
 public static boolean isActivated(String phrase){
  if(phrase==null)return false;
  String text=Normalizer.normalize(phrase,Normalizer.Form.NFD)
     .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
     .replaceAll("[^a-z0-9]+"," ").trim();
  if(text.isEmpty())return false;
  // Whole-word CONTROL, including preceding words e.g. "hola control".
  // Limit input to a brief command to reduce activation by media dialogues.
  String[] tokens=text.split(" ");
  if(tokens.length>14)return false;
  for(int i=0;i<tokens.length;i++){
    boolean wake=tokens[i].equals("control") ||
        (tokens[i].equals("con") && i+1<tokens.length && tokens[i+1].equals("trol"));
    if(!wake)continue;
    // A wake word with no request is not sufficient; bare greeting allowed.
    int start=i+(tokens[i].equals("con")?2:1);
    for(int j=0;j<tokens.length;j++)
      if(j!=i && (tokens[j].equals("hola")||tokens[j].equals("estado")||
          tokens[j].equals("pantalla")||tokens[j].equals("escuchas")||
          tokens[j].equals("escucha")||tokens[j].equals("prueba")||
          tokens[j].equals("ves")||tokens[j].equals("ayuda")||
          tokens[j].equals("estas")||tokens[j].equals("captura")))return true;
    // One or more words following wake can be a meaningful free-form question.
    if(tokens.length-start>=2)return true;
  }
  return false;
 }
}
