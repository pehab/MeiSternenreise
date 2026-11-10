package de.meiapps.sternenreise;
import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import android.os.SystemClock;
import android.view.*;
import android.widget.Button;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class SmokeTest {
    private Button find(View v,String label){if(v instanceof Button&&label.equals(((Button)v).getText().toString()))return (Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button found=find(g.getChildAt(i),label);if(found!=null)return found;}}return null;}
    @Test public void opensRendersAndResumesWithoutSensor() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();
        Activity a=i.startActivitySync(new Intent(i.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        i.waitForIdleSync();assertNotNull(a);
        i.runOnMainSync(()->{Button b=find(a.getWindow().getDecorView(),"Am Handy spielen");assertNotNull(b);b.performClick();});
        SystemClock.sleep(2200);i.waitForIdleSync();
        assertFalse(a.isFinishing());screenshot(i,"phone.png");
        i.runOnMainSync(()->((MainActivity)a).startGame(true));SystemClock.sleep(1200);i.waitForIdleSync();screenshot(i,"stereo.png");
        i.runOnMainSync(a::onBackPressed);i.waitForIdleSync();
        i.runOnMainSync(()->assertNotNull(find(a.getWindow().getDecorView(),"Am Handy spielen")));
        i.runOnMainSync(()->a.recreate());i.waitForIdleSync();
    }
    private void screenshot(Instrumentation i,String name) throws Exception {
        Bitmap image=i.getUiAutomation().takeScreenshot();assertNotNull(image);
        try(FileOutputStream out=new FileOutputStream(new File(i.getTargetContext().getExternalFilesDir(null),name))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
}
