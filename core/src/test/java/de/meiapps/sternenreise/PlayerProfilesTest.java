package de.meiapps.sternenreise;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlayerProfilesTest {
    @Test public void migratesOldGameAndKeepsProgressIndependent(){
        PlayerProfiles p=PlayerProfiles.restore(null,CampaignTest.tutorial().save());String first=p.active().id();assertTrue(Adventure.restore(p.active().state()).finished());
        String child=p.create("Kind");assertFalse(Adventure.restore(p.active().state()).finished());
        Adventure next=new Adventure();next.act("key");p.store(child,next.save());p.select(first);assertTrue(Adventure.restore(p.active().state()).finished());
        p=PlayerProfiles.restore(p.save(),"");assertEquals(first,p.active().id());p.select(child);assertTrue(Adventure.restore(p.active().state()).hasKey());assertFalse(p.select("missing"));
    }
    @Test public void delayedSaveCannotCrossPlayersOrResurrectDeletedProfiles(){
        PlayerProfiles p=PlayerProfiles.restore(null,"");String a=p.active().id(),b=p.create("B");p.store(a,CampaignTest.tutorial().save());assertEquals(b,p.active().id());assertFalse(Adventure.restore(p.active().state()).finished());
        assertTrue(p.delete(a));assertFalse(p.store(a,CampaignTest.tutorial().save()));assertFalse(p.delete(b));assertFalse(p.delete("missing"));
        String c=p.create("C");assertTrue(p.delete(c));assertEquals(b,p.active().id());
    }
    @Test public void namesUnicodeRoundTripsAndLimits(){
        PlayerProfiles p=PlayerProfiles.restore("bad","");String id=p.create("  Peter | 🌙  ");p.rename(id,"Peter 🚀");p=PlayerProfiles.restore(p.save(),"");assertEquals("Peter 🚀",p.active().name());
        try{p.create("peter 🚀");fail("duplicate");}catch(IllegalArgumentException expected){ }
        for(String bad:new String[]{"",null,"ABCDEFGHIJKLMNOPQRSTUVWXYZ"})try{p.create(bad);fail("invalid name");}catch(IllegalArgumentException expected){ }
        try{p.rename("missing","Name");fail("missing");}catch(IllegalArgumentException expected){ }
        while(p.all().size()<8)p.create("Kind "+p.all().size());try{p.create("Neun");fail("limit");}catch(IllegalArgumentException expected){ }
        assertEquals(8,PlayerProfiles.restore(p.save(),"").all().size());
    }
    @Test public void malformedProfileEntriesLeaveValidPlayersIntact(){
        PlayerProfiles p=PlayerProfiles.restore(null,"");p.create("Mia");String valid=p.save();PlayerProfiles restored=PlayerProfiles.restore(valid+"\ninvalid|@@|@@\nmissing-fields","");assertEquals(2,restored.all().size());
        assertEquals(1,PlayerProfiles.restore("PROFILES1|unknown\nbad-data","").all().size());
        assertEquals(2,PlayerProfiles.restore(valid.replace(p.active().id()+"\n","unknown\n"),"").all().size());
    }
}
