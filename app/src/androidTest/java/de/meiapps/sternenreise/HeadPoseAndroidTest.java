package de.meiapps.sternenreise;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class HeadPoseAndroidTest {
    @Test public void displayRemappingAndViewAgreeWithAndroidMatrixConventions(){
        int[] x={SensorManager.AXIS_X,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,SensorManager.AXIS_MINUS_Y};
        int[] y={SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,SensorManager.AXIS_MINUS_Y,SensorManager.AXIS_X};
        float[] reference=new float[16],current=new float[16];
        SensorManager.getRotationMatrixFromVector(reference,new float[]{.1f,.2f,.3f,(float)Math.sqrt(.86)});
        SensorManager.getRotationMatrixFromVector(current,new float[]{-.2f,.1f,-.1f,(float)Math.sqrt(.94)});
        for(int orientation=0;orientation<4;orientation++){
            float[] referenceRows=new float[16],currentRows=new float[16],referenceColumns=new float[16],expected=new float[16],actual=new float[16];
            assertTrue(SensorManager.remapCoordinateSystem(reference,x[orientation],y[orientation],referenceRows));
            assertTrue(SensorManager.remapCoordinateSystem(current,x[orientation],y[orientation],currentRows));
            Matrix.transposeM(referenceColumns,0,referenceRows,0);
            Matrix.multiplyMM(expected,0,currentRows,0,referenceColumns,0);
            HeadPose pose=new HeadPose();pose.update(reference,orientation);pose.update(current,orientation);pose.copyView(actual);
            assertArrayEquals("Android display orientation "+orientation,expected,actual,.00002f);
        }
    }
}
