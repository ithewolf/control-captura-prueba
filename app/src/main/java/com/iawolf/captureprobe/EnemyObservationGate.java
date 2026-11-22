package com.iawolf.captureprobe;

/**
 * Temporal verification of detections from an external vision model.
 * Does not identify champions from pixels: it validates a detector's claims.
 * Missing labels, an ally label, stale frames and inconsistent positions
 * are not confirmed enemies.
 */
public final class EnemyObservationGate {
    public static final class Detection {
        public final String championId;
        public final String team; // ENEMY, ALLY or UNKNOWN from a verified detector
        public final float confidence, xCenter, yCenter;
        public final long frameMillis;
        public Detection(String championId, String team, float confidence,
                         float xCenter, float yCenter, long frameMillis) {
            this.championId=championId;this.team=team;this.confidence=confidence;
            this.xCenter=xCenter;this.yCenter=yCenter;this.frameMillis=frameMillis;
        }
    }
    public static final class Confirmation {
        public final boolean confirmed;
        public final String championId, reason;
        public final float xCenter, yCenter;
        Confirmation(boolean confirmed,String championId,String reason,float x,float y){
            this.confirmed=confirmed;this.championId=championId;this.reason=reason;
            this.xCenter=x;this.yCenter=y;
        }
    }
    private Detection previous;
    public synchronized Confirmation observe(Detection current) {
        if (!valid(current)) {
            previous=null;
            return no("No hay deteccion enemiga confiable.");
        }
        boolean consecutive=previous!=null
            && current.frameMillis>previous.frameMillis
            && current.frameMillis-previous.frameMillis<=1500
            && previous.championId.equals(current.championId)
            && Math.abs(previous.xCenter-current.xCenter)<=0.18f
            && Math.abs(previous.yCenter-current.yCenter)<=0.18f;
        previous=current;
        if (!consecutive) return no("Primera observacion; esperando confirmacion temporal.");
        return new Confirmation(true,current.championId,
            "Enemigo detectado en dos fotogramas consecutivos.",current.xCenter,current.yCenter);
    }
    public synchronized void reset() {previous=null;}
    private boolean valid(Detection d) {
        return d!=null && d.championId!=null && !d.championId.trim().isEmpty()
            && "ENEMY".equals(d.team) && Float.isFinite(d.confidence)
            && d.confidence>=0.80f && d.confidence<=1f
            && Float.isFinite(d.xCenter) && Float.isFinite(d.yCenter)
            && d.xCenter>=0 && d.xCenter<=1 && d.yCenter>=0 && d.yCenter<=1
            && d.frameMillis>=0;
    }
    private Confirmation no(String reason) {
        return new Confirmation(false,"",reason,-1,-1);
    }
}
