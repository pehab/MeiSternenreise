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
        Activity activity=instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();assertNotNull(activity);
        instrumentation.runOnMainSync(()->{
            Button play=find(activity.getWindow().getDecorView(),"Am Handy spielen");assertNotNull(play);play.performClick();
        });
        SystemClock.sleep(1400);instrumentation.waitForIdleSync();
        assertFalse(activity.isFinishing());screenshot(instrumentation,"phone.png");
        instrumentation.runOnMainSync(()->((MainActivity)activity).startGame(true));
        SystemClock.sleep(900);instrumentation.waitForIdleSync();screenshot(instrumentation,"stereo.png");
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
        instrumentation.runOnMainSync(activity::onBackPressed);instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Am Handy spielen")));
        SystemClock.sleep(300);screenshot(instrumentation,"menu.png");
        instrumentation.runOnMainSync(activity::recreate);instrumentation.waitForIdleSync();
        assertTrue(saved(instrumentation).finished());
    }
    private Adventure saved(Instrumentation instrumentation) {
        return Adventure.restore(instrumentation.getTargetContext().getSharedPreferences("adventure",0).getString("state",""));
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
