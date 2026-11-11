package de.meiapps.sternenreise;
import org.junit.Test;
import static org.junit.Assert.*;
public class CampaignTest {
    static Adventure tutorial(){Adventure a=new Adventure();run(a,"key","chest","garden","pot","water","flower","observatory","star","moon","sun","machine");return a;}
    static void run(Adventure a,String... actions){for(String id:actions){a.act(id);assertEquals(a.save(),Adventure.restore(a.save()).save());}}
    static void house(Adventure a){run(a,"begin","clue","key","chest","garden","clue");}
    static void mail(Adventure a){house(a);run(a,"sun","moon","star","observatory","clue","star","machine");}
    static void dew(Adventure a){house(a);run(a,"fill5","pour5to3","empty3","pour5to3","fill5","pour5to3");assertEquals(4,a.jug5());run(a,"flower","observatory","clue","machine");}
    static void clock(Adventure a){house(a);run(a,"oil","repair","observatory","clue","lever_a","lever_b","lever_b","lever_c","lever_c","machine");}
    static void ferry(Adventure a){house(a);run(a,"cargo_1","cross","cross","cargo_0","cross","cargo_1","cross","cargo_2","cross","cross","cargo_1","cross");assertEquals(7,a.banks());run(a,"observatory","clue","moon","star","sun","moon","machine");}
    @Test public void allFiveChaptersHaveSolutionsAndRoundTripEveryAction(){
        Adventure a=tutorial();assertEquals(1,a.unlocked());assertFalse(a.campaignFinished());
        run(a,"next");mail(a);assertTrue(a.finished());run(a,"next");dew(a);assertTrue(a.finished());run(a,"next");clock(a);assertTrue(a.finished());run(a,"next");ferry(a);
        assertTrue(a.campaignFinished());assertEquals(4,a.chapter());run(a,"next");assertEquals(4,a.chapter());
    }
    @Test public void chapterSelectionKeepsSeparateCheckpointsAndUnlockedHistory(){
        Adventure a=tutorial();assertFalse(a.openChapter(4));assertFalse(a.openChapter(-1));run(a,"next");house(a);run(a,"sun");
        String status=a.puzzleStatus();assertTrue(a.openChapter(0));assertTrue(a.finished());assertTrue(a.openChapter(1));assertEquals(status,a.puzzleStatus());
        mailRemaining(a);assertEquals(2,a.unlocked());a.openChapter(0);a.restartChapter();assertFalse(a.finished());assertTrue(a.canOpen(2));
        a=Adventure.restore(a.save());assertTrue(a.canOpen(2));assertTrue(a.completed(1));
    }
    private static void mailRemaining(Adventure a){run(a,"moon","star","observatory","clue","star","machine");}
    @Test public void oldCompletedSaveUnlocksTheNextChapter(){
        Adventure a=Adventure.restore("OBSERVATORY,true,true,true,true,true,true,true,3");assertTrue(a.finished());assertEquals(1,a.unlocked());assertTrue(a.openChapter(1));assertFalse(a.finished());
    }
    @Test public void cluesInventoryAndWrongMovesCannotSkipPuzzles(){
        Adventure a=tutorial();a.act("next");a.act("key");assertFalse(a.hasKey());a.act("hint");a.act("story");a.act("begin");a.act("chest");assertEquals(0,a.stage());
        a.act("key");a.act("chest");assertEquals(0,a.stage());a.act("garden");assertEquals(Adventure.Room.WORKSHOP,a.room());
        run(a,"clue","chest","garden");a.act("sun");assertEquals(0,a.symbols());a.act("observatory");assertEquals(Adventure.Room.GARDEN,a.room());
        run(a,"clue","star");assertEquals(0,a.symbols());run(a,"sun","moon","star","sun","observatory");a.act("machine");assertFalse(a.finished());a.act("star");assertEquals(2,a.stage());a.act("clue");a.act("moon");assertEquals(2,a.stage());run(a,"star","machine");assertTrue(a.finished());
    }
    @Test public void jugMathIsConservativeAndSelectionIsSaved(){
        Adventure a=tutorial();a.act("next");mail(a);a.act("next");house(a);
        a.act("fill");assertEquals(0,a.jug5());run(a,"select5","fill","pour");assertEquals(2,a.jug5());assertEquals(3,a.jug3());
        a.act("flower");assertEquals(1,a.stage());run(a,"select3","empty","pour","fill","empty","select5","empty","reset_puzzle");assertEquals(0,a.jug3());assertEquals(0,a.jug5());
        run(a,"fill3","pour3to5");assertEquals(3,a.jug5());assertEquals(0,a.jug3());
        run(a,"back");assertEquals(Adventure.Room.WORKSHOP,a.room());run(a,"garden");assertEquals(3,a.jug5());
    }
    @Test public void clockIsCoupledAndFerryRejectsUnsafeBanks(){
        Adventure a=tutorial();a.act("next");mail(a);a.act("next");dew(a);a.act("next");house(a);a.act("repair");assertEquals(1,a.stage());run(a,"oil","repair","observatory");a.act("machine");assertFalse(a.finished());run(a,"clue","machine");assertFalse(a.finished());
        a.act("lever_a");assertEquals(1,a.wheel(0));assertEquals(1,a.wheel(1));assertEquals(0,a.wheel(2));run(a,"reset_puzzle","lever_a","lever_b","lever_b","lever_c","lever_c","machine","next");house(a);
        String original=a.save();a.act("cross");assertEquals(original,a.save());a.act("cargo_0");String fox=a.save();a.act("cross");assertEquals(fox,a.save());
        a.act("cargo_0");assertEquals(-1,a.cargo());run(a,"cargo_1","cross");assertEquals(1,a.boatSide());a.act("cargo_0");assertEquals(-1,a.cargo());
        assertFalse(Adventure.safeBanks(3,0));assertFalse(Adventure.safeBanks(6,0));assertTrue(Adventure.safeBanks(5,0));
    }
    @Test public void progressiveHintsAndAllRoomNarrativesRemainAvailable(){
        Adventure a=tutorial();for(int chapter=1;chapter<5;chapter++){
            a.act("next");assertFalse(a.opening().isEmpty());a.act("story");a.act("begin");assertTrue(a.introduced());
            for(int n=0;n<4;n++)assertFalse(a.act("hint").isEmpty());assertFalse(a.act("unknown").isEmpty());
            a.act("clue");a.act("key");a.act("chest");a.act("garden");for(int n=0;n<4;n++)a.act("hint");a.act("clue");
            if(chapter==1)run(a,"sun","moon","star");else if(chapter==2)run(a,"fill5","pour5to3","empty3","pour5to3","fill5","pour5to3","flower");else if(chapter==3)run(a,"oil","repair");else run(a,"cargo_1","cross","cross","cargo_0","cross","cargo_1","cross","cargo_2","cross","cross","cargo_1","cross");
            assertFalse(a.puzzleStatus().isEmpty()&&chapter!=3);a.act("observatory");a.act("clue");for(int n=0;n<4;n++)a.act("hint");
            if(chapter==1)run(a,"star");else if(chapter==3)run(a,"lever_a","lever_b","lever_b","lever_c","lever_c");else if(chapter==4)run(a,"star","moon","star","sun","moon","star");
            a.act("machine");assertTrue(a.finished());a.act("back");a.act("back");assertEquals(Adventure.Room.WORKSHOP,a.room());
        }
    }
}
