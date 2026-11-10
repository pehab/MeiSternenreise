package de.meiapps.sternenreise;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdventureTest {
    private Adventure garden(){Adventure a=new Adventure();a.act("key");a.act("chest");a.act("garden");return a;}
    private Adventure observatory(){Adventure a=garden();a.act("pot");a.act("water");a.act("flower");a.act("observatory");return a;}
    @Test public void doorsAndChestRespectProgression(){
        Adventure a=new Adventure();assertEquals(Adventure.Room.WORKSHOP,a.room());assertFalse(a.hasKey());assertFalse(a.hasSeed());
        assertTrue(a.hint().contains("Schlüssel"));a.act("garden");a.act("chest");assertFalse(a.hasSeed());
        a.act("key");assertTrue(a.hasKey());assertTrue(a.hint().contains("Truhe"));a.act("chest");assertTrue(a.hasSeed());assertTrue(a.hint().contains("Tür"));
        a.act("garden");assertEquals(Adventure.Room.GARDEN,a.room());assertTrue(a.title().contains("Mondgarten"));
        a.act("observatory");assertEquals(Adventure.Room.GARDEN,a.room());a.act("flower");assertFalse(a.hasCrystal());
        a.act("water");assertFalse(a.watered());a.act("pot");assertTrue(a.planted());a.act("water");assertTrue(a.watered());
        assertTrue(a.hint().contains("Kristall"));a.act("pot");a.act("flower");assertTrue(a.hasCrystal());assertTrue(a.hint().contains("Tür"));
        a.act("observatory");assertEquals(Adventure.Room.OBSERVATORY,a.room());assertTrue(a.title().contains("Sternwarte"));
        a.act("machine");assertFalse(a.finished());
    }
    @Test public void puzzleIsRecoverableAndFinishPersists(){
        Adventure a=observatory();assertFalse(a.hasLens());assertTrue(a.hint().contains("Stern, Mond, Sonne"));
        a.act("moon");assertEquals(0,a.symbols());a.act("star");a.act("star");assertEquals(1,a.symbols());
        a.act("sun");assertEquals(0,a.symbols());a.act("star");a.act("moon");a.act("sun");assertEquals(3,a.symbols());assertTrue(a.hasLens());
        assertTrue(a.hint().contains("maschine"));a.act("sun");a.act("machine");assertTrue(a.finished());
        a=Adventure.restore(a.save());assertTrue(a.finished());assertTrue(a.hint().contains("zurückgebracht"));
        a.act("back");assertEquals(Adventure.Room.GARDEN,a.room());a.act("back");assertEquals(Adventure.Room.WORKSHOP,a.room());assertEquals("Die Sternenwerkstatt",a.title());
    }
    @Test public void everyCheckpointRoundTrips(){
        Adventure a=new Adventure();for(String action:new String[]{"hint","unknown","key","chest","garden","unknown","pot","water","flower","observatory","unknown","star","moon","sun","machine","back","back"}){
            a.act(action);Adventure b=Adventure.restore(a.save());assertEquals(a.save(),b.save());assertEquals(a.hint(),b.act("hint"));
        }
    }
    @Test public void corruptOrImpossibleSavesResetSafely(){
        String fresh=new Adventure().save();for(String value:new String[]{"","garbage",null,"NOWHERE,false,false,false,false,false,false,false,0","WORKSHOP,false,false,false,false,false,false,false,nope","WORKSHOP,yes,false,false,false,false,false,false,0", "WORKSHOP,false,true,false,false,false,false,false,0", "WORKSHOP,false,false,true,false,false,false,false,0", "WORKSHOP,true,true,false,true,false,false,false,0", "WORKSHOP,true,true,true,false,true,false,false,0", "WORKSHOP,true,true,true,true,false,true,false,3", "WORKSHOP,true,true,true,true,true,false,false,3", "WORKSHOP,true,true,true,true,true,false,true,0", "GARDEN,false,false,false,false,false,false,false,0", "OBSERVATORY,true,true,false,false,false,false,false,0", "WORKSHOP,false,false,false,false,false,false,false,-1", "WORKSHOP,false,false,false,false,false,false,false,4"}) assertEquals(String.valueOf(value),fresh,Adventure.restore(value).save());
    }
}
