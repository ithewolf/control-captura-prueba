package com.iawolf.captureprobe;

/** JVM-only smoke test: no Android runtime or microphone access needed. */
public final class AvContextSmokeTest {
 private static void check(boolean ok,String name) {
  if(!ok)throw new AssertionError(name);
  System.out.println("PASS "+name);
 }
 public static void main(String[] args){
  AvContext.reset(100L);
  check(!AvContext.snapshot(100L,1000L).fresh,"no frame is unknown");
  AvContext.recordFrame(100L,1000L,1L,0L,false);
  check(AvContext.snapshot(100L,1100L).fresh,"fresh frame accepted");
  check(!AvContext.snapshot(100L,4000L).fresh,"stale frame rejected");
  check(!AvContext.snapshot(101L,1100L).fresh,"different session rejected");
  AvContext.recordFrame(99L,1150L,2L,1L,false);
  check(AvContext.snapshot(100L,1200L).frameNumber==1L,"old session ignored");
  AvContext.recordFrame(100L,900L,3L,1L,false);
  check(AvContext.snapshot(100L,1200L).frameNumber==1L,"out-of-order frame ignored");
  check(AvContext.respond("hola",AvContext.snapshot(100L,1200L))==null,"wake word required");
  check(AvContext.respond("el control del juego",AvContext.snapshot(100L,1200L))==null,"background mentions ignored");
  check(AvContext.respond("descontrol total",AvContext.snapshot(100L,1200L))==null,"substring ignored");
  check(AvContext.respond("CONTROL, qué ves",AvContext.snapshot(100L,1200L))!=null,"explicit command accepted");
  check(AvContext.respond("CONTROL, qué ves",AvContext.snapshot(100L,5000L)).contains("no tengo una imagen reciente"),"no fabricated vision");
  AvContext.reset(101L);
  check(!AvContext.snapshot(101L,1300L).fresh,"reset clears stale context");
 }
}
