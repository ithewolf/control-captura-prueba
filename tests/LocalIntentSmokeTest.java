package com.iawolf.captureprobe;
public class LocalIntentSmokeTest {
 private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args){
  check(LocalIntent.reply("hola me escuchas bien","companero",true,false,true).contains("te escucho"),"hearing");
  check(LocalIntent.reply("que modo estas","coach",true,false,true).contains("Coach"),"coach");
  check(LocalIntent.reply("que modo estas","companero",true,false,true).contains("Compañero"),"companion");
  check(LocalIntent.reply("control que ves en la pantalla","coach",true,false,true).contains("campeones"),"visual limitation");
  check(LocalIntent.reply("necesito consejo de jungla","coach",true,false,true).contains("no tengo visión"),"no fabricated tactics");
  check(LocalIntent.reply("hola","coach",true,false,true)!=null,"greeting");
  check(LocalIntent.reply("tengo una imagen del juego","companero",true,false,true)==null,"unknown phrase must not trigger generic reply");
  System.out.println("LocalIntentSmokeTest OK");
 }
}
