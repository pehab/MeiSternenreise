package de.meiapps.sternenreise;
import org.junit.Test;
import static org.junit.Assert.*;
public class GazeTest {
    @Test public void activatesOnceAndRequiresLeaving(){Gaze g=new Gaze();assertFalse(g.update("door",100));assertFalse(g.update("door",1899));assertEquals(.5f,g.progress(1000),.001f);assertTrue(g.update("door",1900));assertFalse(g.update("door",9000));assertEquals(1,g.progress(9000),0);assertFalse(g.update(null,10000));assertEquals(0,g.progress(10000),0);assertFalse(g.update("door",10000));assertTrue(g.update("door",11800));}
    @Test public void switchPauseAndClockResetRestartDwell(){Gaze g=new Gaze();g.update("one",0);assertFalse(g.update("two",1700));assertFalse(g.update("two",1800));g.reset();assertFalse(g.update("two",3600));assertFalse(g.update("two",100));assertEquals(0,g.progress(0),0);assertTrue(g.update("two",1900));}
    @Test public void raysHitInAllDirections(){assertTrue(Gaze.hits(new float[]{0,0,-1},0,0,-4,.1f));assertFalse(Gaze.hits(new float[]{0,0,-1},0,0,4,.1f));assertFalse(Gaze.hits(new float[]{0,0,-1},0,0,0,.1f));assertFalse(Gaze.hits(new float[]{0,0,-1},1,0,-4,.1f));assertTrue(Gaze.hits(new float[]{1,0,0},4,0,0,.1f));}
}
