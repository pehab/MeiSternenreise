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
        instrumentation.runOnMainSync(activity::onBackPressed);instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Am Handy spielen")));
        SystemClock.sleep(300);screenshot(instrumentation,"menu.png");
        instrumentation.runOnMainSync(activity::recreate);instrumentation.waitForIdleSync();
        assertTrue(saved(instrumentation).finished());
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
