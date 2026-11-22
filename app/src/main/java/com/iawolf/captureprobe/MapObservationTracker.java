package com.iawolf.captureprobe;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure-Java temporal state for externally validated minimap detections.
 * NEVER extracts pixels, predicts hidden enemies, or identifies champions.
 * One instance per match; clear on match/orientation change.
 */
public final class MapObservationTracker {
    public enum Team { ALLY, ENEMY }
    public enum Visibility { VISIBLE, LAST_SEEN, UNKNOWN }
    private static final long VISIBLE_TTL_MS = 1200;
    private static final long LAST_SEEN_TTL_MS = 12000;
    private static final double MIN_CONFIDENCE = 0.65;
    private static final int MAX_TRACKS = 10;
    private final Map<String, Observation> observations = new HashMap<>();
    private long latestTimestamp = -1;
    public static final class Observation {
        public final String id;
        public final Team team;
        public final double x, y, confidence;
        public final long timestampMs;
        private Observation(String id,Team team,double x,double y,double confidence,long timestampMs) {
            this.id=id; this.team=team; this.x=x; this.y=y; this.confidence=confidence; this.timestampMs=timestampMs;
        }
    }
    public static final class State {
        public final Visibility visibility;
        public final Team team;
        public final double x,y;
        public final long ageMs;
        private State(Visibility visibility,Team team,double x,double y,long ageMs) {
            this.visibility=visibility;this.team=team;this.x=x;this.y=y;this.ageMs=ageMs;
        }
    }
    public synchronized boolean observe(String id,Team team,double x,double y,double confidence,long timestampMs) {
        if(id==null||id.length()==0||id.length()>64||team==null||timestampMs<0||
           !Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1||
           !Double.isFinite(confidence)||confidence<MIN_CONFIDENCE||confidence>1) return false;
        if(latestTimestamp>=0&&timestampMs<latestTimestamp) return false;
        Observation previous=observations.get(id);
        if(previous!=null&&previous.team!=team) return false;
        if(previous==null&&observations.size()>=MAX_TRACKS) return false;
        observations.put(id,new Observation(id,team,x,y,confidence,timestampMs));
        latestTimestamp=timestampMs;
        return true;
    }
    public synchronized State state(String id,long nowMs) {
        Observation o=observations.get(id);
        if(o==null||nowMs<o.timestampMs) return new State(Visibility.UNKNOWN,null,Double.NaN,Double.NaN,-1);
        long age=nowMs-o.timestampMs;
        if(age>LAST_SEEN_TTL_MS) return new State(Visibility.UNKNOWN,o.team,Double.NaN,Double.NaN,age);
        return new State(age<=VISIBLE_TTL_MS?Visibility.VISIBLE:Visibility.LAST_SEEN,o.team,o.x,o.y,age);
    }
    public synchronized void clear(){observations.clear();latestTimestamp=-1;}
    public synchronized int trackCount(){return observations.size();}
}
