package com.iawolf.captureprobe;
public final class WakeGateSmokeTest {
 private static void yes(String s){if(!WakeGate.isActivated(s))throw new AssertionError("rejected: "+s);}
 private static void no(String s){if(WakeGate.isActivated(s))throw new AssertionError("unexpected wake: "+s);}
 public static void main(String[] args){
   yes("control hola");yes("hola control");yes("control que ves");
   yes("hola con trol que ves");yes("wolf control estas ahi");
   no("golf soy con hola wolf");no("el jugador tiene control de su campeón");
   no("control");no("hola");no("");
   System.out.println("WakeGate tests OK");
 }
}
