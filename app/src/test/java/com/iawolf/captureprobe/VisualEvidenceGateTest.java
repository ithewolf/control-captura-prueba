package com.iawolf.captureprobe;
import java.util.Arrays;
import java.util.List;

/** Ensayos de rechazo. No hacen inferencia visual ni miden precisión real. */
public final class VisualEvidenceGateTest {
  private static int count=0;
  private static void ok(boolean value,String label) {
    if(!value)throw new AssertionError(label);
    count++;
  }
  private static VisualEvidenceGate.Observation obs(String track,
    VisualEvidenceGate.Label label,VisualEvidenceGate.Source src,
    float conf,long ms,long frame){
    return new VisualEvidenceGate.Observation(track,label,src,conf,ms,frame);
  }
  public static void main(String[] args){
    VisualEvidenceGate.Label E=VisualEvidenceGate.Label.ENEMY;
    VisualEvidenceGate.Label A=VisualEvidenceGate.Label.ALLY;
    VisualEvidenceGate.Label N=VisualEvidenceGate.Label.NEUTRAL;
    VisualEvidenceGate.Label U=VisualEvidenceGate.Label.UNKNOWN;
    VisualEvidenceGate.Source H=VisualEvidenceGate.Source.RAW_COLORS;
    VisualEvidenceGate.Source V=VisualEvidenceGate.Source.VALIDATED_MODEL;
    List<VisualEvidenceGate.Observation> data=Arrays.asList(
      obs("a",E,H,0.99f,1000,1),obs("a",E,H,0.99f,1100,2));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==U,"color heurístico no verifica enemigo");
    data=Arrays.asList(obs("a",E,V,.94f,1200,1));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1300)==U,"un frame no basta");
    data=Arrays.asList(obs("a",E,V,.94f,1200,1),obs("a",E,V,.96f,1400,2));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==E,"dos cuadros válidos");
    ok(VisualEvidenceGate.verifyUnit(data,"a",4001)==U,"caducidad");
    ok(VisualEvidenceGate.verifyUnit(data,"b",1500)==U,"identidad distinta");
    data=Arrays.asList(obs("a",E,V,.94f,1200,1),obs("a",A,V,.95f,1300,2));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==U,"contradicción");
    data=Arrays.asList(obs("a",E,V,.6f,1200,1),obs("a",E,V,.95f,1300,2));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==U,"baja confianza");
    data=Arrays.asList(obs("a",N,V,.96f,1200,1),obs("a",N,V,.94f,1300,1));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==U,"repetición del mismo cuadro");
    data=Arrays.asList(obs("a",N,V,.95f,1600,1),obs("a",N,V,.95f,1400,2));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==U,"observaciones del futuro");
    data=Arrays.asList(obs("a",N,V,.95f,1200,1),obs("a",N,V,.95f,1300,2));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==N,"neutral coherente");
    data=Arrays.asList(obs("a",E,V,Float.NaN,1200,1),obs("a",E,V,.95f,1300,2));
    ok(VisualEvidenceGate.verifyUnit(data,"a",1500)==U,"confianza inválida");
    ok(VisualEvidenceGate.verifyUnit(null,"a",1500)==U,"entrada nula");
    System.out.println("VisualEvidenceGateTest: "+count+" pruebas correctas");
  }
}
