package com.iawolf.captureprobe;

/**
 * Converts validated map icon screen positions into a consistent map orientation.
 * Orientation MUST be known independently; guessing the team-side would invent positions.
 * No image analysis or champion detection happens here.
 */
public final class MinimapCoordinateNormalizer {
    public enum Orientation { NORMAL, ROTATED_180, UNKNOWN }
    public static final class Point {
        public final double x,y;
        public Point(double x,double y){this.x=x;this.y=y;}
    }
    private MinimapCoordinateNormalizer(){}
    public static Point normalize(double x,double y,Orientation orientation) {
        if(orientation==null||orientation==Orientation.UNKNOWN) return null;
        if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1) return null;
        return orientation==Orientation.ROTATED_180 ? new Point(1-x,1-y) : new Point(x,y);
    }
}
