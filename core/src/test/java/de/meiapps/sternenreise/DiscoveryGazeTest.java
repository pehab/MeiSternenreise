package de.meiapps.sternenreise;
import org.junit.Test;
import static org.junit.Assert.*;
public class DiscoveryGazeTest {
    @Test public void enteringAnAlreadyFocusedObjectNeverCollectsIt(){Gaze g=new Gaze();g.requireNewLook();assertTrue(g.needsNewLook());assertFalse(g.update("key",0));assertFalse(g.update("key",30000));assertEquals(0,g.progress(30000),0);assertFalse(g.update(null,30001));assertFalse(g.needsNewLook());assertFalse(g.update("key",30002));assertTrue(g.update("key",31802));assertFalse(g.update("key",50000));}
    @Test public void anObjectAppearingAfterAnActionNeedsANewLook(){Gaze g=new Gaze();g.update("pot",0);assertTrue(g.update("pot",1800));g.requireNewLook();assertFalse(g.update("flower",2000));assertFalse(g.update("flower",10000));g.update(null,10001);g.update("flower",10002);assertTrue(g.update("flower",11802));}
}
