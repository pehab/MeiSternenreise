package de.meiapps.sternenreise;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SmokeTest {
    private Button find(View view,String label) {
        if(view instanceof Button && label.equals(((Button)view).getText().toString()))return (Button)view;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int index=0;index<group.getChildCount();index++) {
                Button found=find(group.getChildAt(index),label);if(found!=null)return found;
            }
        }
        return null;
    }
    @Test public void rendersEveryLocationAndCompletesAdventureInBothModes() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        instrumentation.getTargetContext().getSharedPreferences("adventure",0).edit().clear().commit();
        Activity activity=instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();assertNotNull(activity);
        instrumentation.runOnMainSync(()->{
            Button play=find(activity.getWindow().getDecorView(),"Am Handy spielen");assertNotNull(play);play.performClick();
        });
        SystemClock.sleep(3300);instrumentation.waitForIdleSync();
        assertFalse("Looking forward must not collect the hidden key",saved(instrumentation).hasKey());
        assertFalse(activity.isFinishing());screenshot(instrumentation,"phone.png");
        instrumentation.runOnMainSync(()->((MainActivity)activity).startGame(true));
        SystemClock.sleep(900);instrumentation.waitForIdleSync();screenshot(instrumentation,"stereo.png");
        instrumentation.getTargetContext().getSharedPreferences("adventure",0).edit().putFloat("vrScreenWidthMm",150).putFloat("ipd",.064f).putFloat("vrVerticalMm",0).commit();
        instrumentation.runOnMainSync(()->((MainActivity)activity).startCalibration());
        SystemClock.sleep(2000);instrumentation.waitForIdleSync();
        checkAlignment(instrumentation);screenshot(instrumentation,"alignment.png");
        instrumentation.runOnMainSync(()->activity.onKeyDown(android.view.KeyEvent.KEYCODE_VOLUME_UP,new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_VOLUME_UP)));
        assertEquals(.0645f,instrumentation.getTargetContext().getSharedPreferences("adventure",0).getFloat("ipd",0),.00001f);
        instrumentation.getTargetContext().getSharedPreferences("adventure",0).edit().remove("vrScreenWidthMm").putFloat("ipd",.064f).apply();
        instrumentation.runOnMainSync(()->((MainActivity)activity).startGame(false));
        SystemClock.sleep(600);
        activate(instrumentation,activity,"key");activate(instrumentation,activity,"chest");
        SystemClock.sleep(200);screenshot(instrumentation,"open-chest.png");
        activate(instrumentation,activity,"garden");
        assertEquals(Adventure.Room.GARDEN,saved(instrumentation).room());
        SystemClock.sleep(600);screenshot(instrumentation,"garden.png");
        activate(instrumentation,activity,"pot");
        SystemClock.sleep(200);screenshot(instrumentation,"sprout.png");
        activate(instrumentation,activity,"water");activate(instrumentation,activity,"flower");
        SystemClock.sleep(300);screenshot(instrumentation,"flower.png");
        activate(instrumentation,activity,"observatory");
        assertEquals(Adventure.Room.OBSERVATORY,saved(instrumentation).room());
        SystemClock.sleep(600);screenshot(instrumentation,"observatory.png");
        activate(instrumentation,activity,"star");activate(instrumentation,activity,"moon");activate(instrumentation,activity,"sun");activate(instrumentation,activity,"machine");
        assertTrue(saved(instrumentation).finished());
        SystemClock.sleep(400);screenshot(instrumentation,"ending.png");
        completeNewChapters(instrumentation,activity);
        instrumentation.runOnMainSync(activity::onBackPressed);instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Am Handy spielen")));
        SystemClock.sleep(300);screenshot(instrumentation,"menu.png");
        profileRoundTrip(instrumentation,activity);
        instrumentation.runOnMainSync(activity::recreate);instrumentation.waitForIdleSync();
        assertTrue(saved(instrumentation).finished());
    }
    private void profileRoundTrip(Instrumentation i,Activity a){
        i.runOnMainSync(()->{Button profile=find(a.getWindow().getDecorView(),"Profil wechseln · Spieler 1");assertNotNull(profile);profile.performClick();});
        clickText(i,"Neues Profil");
        android.view.accessibility.AccessibilityNodeInfo root=i.getUiAutomation().getRootInActiveWindow();
        android.view.accessibility.AccessibilityNodeInfo input=findClass(root,"android.widget.EditText");assertNotNull(input);
        android.os.Bundle arguments=new android.os.Bundle();arguments.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Testkind");
        assertTrue(input.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,arguments));clickText(i,"Speichern");
        PlayerProfiles profiles=profileData(i);assertEquals(2,profiles.all().size());assertEquals("Testkind",profiles.active().name());assertFalse(saved(i).hasKey());
        i.runOnMainSync(()->((MainActivity)a).startGame(false));SystemClock.sleep(500);activate(i,a,"key");assertTrue(saved(i).hasKey());
        i.runOnMainSync(a::onBackPressed);i.waitForIdleSync();
        i.runOnMainSync(()->find(a.getWindow().getDecorView(),"Profil wechseln · Testkind").performClick());clickText(i,"Spieler 1");
        assertTrue(saved(i).campaignFinished());assertEquals("Spieler 1",profileData(i).active().name());
        for(PlayerProfiles.Profile player:profileData(i).all())if(player.name().equals("Testkind"))assertTrue(Adventure.restore(player.state()).hasKey());
    }
    private PlayerProfiles profileData(Instrumentation i){android.content.SharedPreferences prefs=i.getTargetContext().getSharedPreferences("adventure",0);return PlayerProfiles.restore(prefs.getString("profiles",null),prefs.getString("state",""));}
    private android.view.accessibility.AccessibilityNodeInfo findClass(android.view.accessibility.AccessibilityNodeInfo node,String name){
        if(node==null)return null;if(name.contentEquals(node.getClassName()))return node;
        for(int index=0;index<node.getChildCount();index++){android.view.accessibility.AccessibilityNodeInfo found=findClass(node.getChild(index),name);if(found!=null)return found;}return null;
    }
    private void clickText(Instrumentation i,String text){
        long deadline=SystemClock.uptimeMillis()+4000;
        while(SystemClock.uptimeMillis()<deadline){
            android.view.accessibility.AccessibilityNodeInfo root=i.getUiAutomation().getRootInActiveWindow();
            if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text)){
                for(int depth=0;node!=null&&depth<5;depth++,node=node.getParent())if(node.isClickable()&&node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)){SystemClock.sleep(250);i.waitForIdleSync();return;}
            }
            SystemClock.sleep(50);
        }fail("Missing dialog action: "+text);
    }
    private void completeNewChapters(Instrumentation i,Activity a) throws Exception {
        for(int chapter=1;chapter<Adventure.CHAPTERS;chapter++){
            activate(i,a,"next");assertEquals(chapter,saved(i).chapter());
            activate(i,a,"begin");activate(i,a,"clue");activate(i,a,"key");activate(i,a,"chest");activate(i,a,"garden");activate(i,a,"clue");
            SystemClock.sleep(600);screenshot(i,"chapter-"+(chapter+1)+".png");
            String[] actions=switch(chapter){
                case 1->new String[]{"sun","moon","star","observatory","clue","star","machine"};
                case 2->new String[]{"select5","fill","pour","select3","empty","select5","pour","fill","pour","flower","observatory","clue","machine"};
                case 3->new String[]{"oil","repair","observatory","clue","lever_a","lever_b","lever_b","lever_c","lever_c","machine"};
                default->new String[]{"cargo_1","cross","cross","cargo_0","cross","cargo_1","cross","cargo_2","cross","cross","cargo_1","cross","observatory","clue","moon","star","sun","moon","machine"};
            };
            for(String id:actions)activate(i,a,id);
            assertTrue("Chapter "+chapter+" must be winnable",saved(i).finished());
        }
        assertTrue(saved(i).campaignFinished());SystemClock.sleep(400);screenshot(i,"campaign-ending.png");
    }
    private void checkAlignment(Instrumentation instrumentation){
        Bitmap image=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(image);
        assertTrue("VR surface must remain landscape",image.getWidth()>image.getHeight());
        double[] sumX=new double[2],sumY=new double[2];int[] count=new int[2];
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int color=image.getPixel(x,y);
            if(android.graphics.Color.green(color)>225&&android.graphics.Color.red(color)<110&&android.graphics.Color.blue(color)>120){
                int eye=x<image.getWidth()/2?0:1;sumX[eye]+=x;sumY[eye]+=y;count[eye]++;
            }
        }
        assertTrue("Both optical-center crosses must be visible",count[0]>20&&count[1]>20);
        assertEquals("Same vertical optical axis",sumY[0]/count[0],sumY[1]/count[1],2);
        double relativeDistance=(sumX[1]/count[1]-sumX[0]/count[0])/image.getWidth();
        assertEquals("64 mm lenses on a 150 mm screen",64.0/150,relativeDistance,.004);
        image.recycle();
    }
    private Adventure saved(Instrumentation instrumentation) {
        android.content.SharedPreferences prefs=instrumentation.getTargetContext().getSharedPreferences("adventure",0);
        return Adventure.restore(PlayerProfiles.restore(prefs.getString("profiles",null),prefs.getString("state","")).active().state());
    }
    private void activate(Instrumentation instrumentation,Activity activity,String action) {
        String before=saved(instrumentation).save();
        instrumentation.runOnMainSync(()->((MainActivity)activity).activate(action));
        long deadline=SystemClock.uptimeMillis()+4000;
        while(before.equals(saved(instrumentation).save()) && SystemClock.uptimeMillis()<deadline)SystemClock.sleep(25);
        assertNotEquals("Action must reach the renderer and persist: "+action,before,saved(instrumentation).save());
        instrumentation.waitForIdleSync();
    }
    private void screenshot(Instrumentation instrumentation,String name) throws Exception {
        Bitmap image=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(image);
        try(FileOutputStream output=new FileOutputStream(new File(instrumentation.getTargetContext().getExternalFilesDir(null),name))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));
        }finally{image.recycle();}
    }
}
