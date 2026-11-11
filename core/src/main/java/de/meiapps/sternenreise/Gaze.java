package de.meiapps.sternenreise;

/** Monotonic dwell: one activation per visit, never repeats while still looking. */
public final class Gaze {
    public static final long DWELL_MS = 1800;
    private String target;
    private long start;
    private boolean fired;
    private boolean needsNewLook;
    public void requireNewLook(){reset();needsNewLook=true;}
    public boolean needsNewLook(){return needsNewLook;}
    public boolean update(String id, long now) {
        if(needsNewLook){if(id==null)needsNewLook=false;return false;}
        if (id == null) { reset(); return false; }
        if (!id.equals(target)) { target=id; start=now; fired=false; }
        if (now < start) { start=now; fired=false; }
        if (!fired && now-start>=DWELL_MS) { fired=true; return true; }
        return false;
    }
    public float progress(long now) { return target==null ? 0 : Math.max(0, Math.min(1, (now-start)/(float)DWELL_MS)); }
    public void reset() { target=null; fired=false; }
    public static boolean hits(float[] direction, float x, float y, float z, float radius) {
        double length=Math.sqrt(x*x+y*y+z*z);
        if (length==0) return false;
        return (direction[0]*x+direction[1]*y+direction[2]*z)/length >= Math.cos(radius);
    }
}
