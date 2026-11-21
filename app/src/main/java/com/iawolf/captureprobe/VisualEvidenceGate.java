package com.iawolf.captureprobe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gate para observaciones visuales. No detecta objetos: examina la procedencia
 * y consistencia de resultados de un detector entrenado y validado.
 *
 * No usar para aprobar un modelo: las métricas de precision/recall se miden
 * offline y en dispositivo antes de conectar la fuente VALIDATED_MODEL.
 */
public final class VisualEvidenceGate {
  public enum Label { ALLY, ENEMY, NEUTRAL, UNKNOWN }
  public enum Source { RAW_COLORS, VALIDATED_MODEL }
  public static final long MAX_AGE_MS=2000L;
  public static final float MIN_CONFIDENCE=0.85f;

  public static final class Observation {
    public final String trackId;
    public final Label label;
    public final Source source;
    public final float confidence;
    public final long timestampMs;
    public final long frameId;
    public Observation(String trackId, Label label, Source source, float confidence,
                       long timestampMs, long frameId) {
      this.trackId=trackId;
      this.label=label;
      this.source=source;
      this.confidence=confidence;
      this.timestampMs=timestampMs;
      this.frameId=frameId;
    }
  }

  private VisualEvidenceGate(){}

  /** Abstenerse ante un solo frame, etiquetas contradictorias o fuente heurística. */
  public static Label verifyUnit(List<Observation> observations,String trackId,long nowMs) {
    if(observations==null || trackId==null || trackId.isEmpty() || nowMs<0)
      return Label.UNKNOWN;
    Label agreed=Label.UNKNOWN;
    Set<Long> frames=new HashSet<>();
    for(Observation o:observations){
      if(o==null || !trackId.equals(o.trackId) || o.label==null || o.source!=Source.VALIDATED_MODEL ||
         !Float.isFinite(o.confidence) || o.confidence<MIN_CONFIDENCE ||
         o.timestampMs<0 || o.timestampMs>nowMs || nowMs-o.timestampMs>MAX_AGE_MS ||
         o.label==Label.UNKNOWN)continue;
      if(agreed!=Label.UNKNOWN && agreed!=o.label)return Label.UNKNOWN;
      agreed=o.label;
      frames.add(o.frameId);
    }
    return frames.size()>=2?agreed:Label.UNKNOWN;
  }
}
