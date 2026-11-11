package de.meiapps.sternenreise;
import org.junit.Test;
import static org.junit.Assert.*;
public class HeadPoseTest {
    private static final float EPS=.00002f;
    private static float[] identity(){return new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};}
    private static float[] multiply(float[] a,float[] b){float[] r=new float[16];for(int y=0;y<4;y++)for(int x=0;x<4;x++)for(int i=0;i<4;i++)r[y*4+x]+=a[y*4+i]*b[i*4+x];return r;}
    private static float[] transpose(float[] a){float[] r=new float[16];for(int y=0;y<4;y++)for(int x=0;x<4;x++)r[y*4+x]=a[x*4+y];return r;}
    private static float[] rotation(int axis,float degrees){float[] r=identity();float c=(float)Math.cos(Math.toRadians(degrees)),s=(float)Math.sin(Math.toRadians(degrees));int a=(axis+1)%3,b=(axis+2)%3;r[a*4+a]=r[b*4+b]=c;r[a*4+b]=-s;r[b*4+a]=s;return r;}
    private static float[] raw(float[] screen,int quarter){return multiply(screen,transpose(rotation(2,-90*quarter)));}
    private static float[] view(HeadPose pose){float[] r=new float[16];pose.copyView(r);return r;}
    private static void same(float[] expected,float[] actual){for(int i=0;i<16;i++)assertEquals("matrix element "+i,expected[i],actual[i],EPS);}
    @Test public void yawPitchAndRollStayOnTheirOwnAxesInEveryDisplayOrientation(){
        float[] reference=multiply(rotation(0,79),multiply(rotation(2,27),rotation(1,-13)));
        for(int quarter=0;quarter<4;quarter++)for(int axis=0;axis<3;axis++) {
            HeadPose pose=new HeadPose();pose.update(raw(reference,quarter),quarter);same(identity(),view(pose));
            float[] local=rotation(axis,30);pose.update(raw(multiply(reference,local),quarter),quarter);
            // A row-major active local rotation is the column-major inverse camera view.
            same(local,view(pose));
        }
    }
    @Test public void nonCommutingMovementUsesReferenceBeforeCurrent(){
        float[] reference=multiply(rotation(0,90),rotation(2,90));
        float[] movement=multiply(rotation(1,43),rotation(0,-18));HeadPose pose=new HeadPose();
        pose.update(reference,0);pose.update(multiply(reference,movement),0);same(movement,view(pose));
    }
    @Test public void recenterAndDisplayChangesDiscardTheOldFrame(){
        HeadPose pose=new HeadPose();pose.update(identity(),0);pose.update(rotation(1,45),0);same(rotation(1,45),view(pose));
        pose.reset();same(identity(),view(pose));pose.update(rotation(1,45),0);same(identity(),view(pose));
        pose.update(rotation(1,90),0);same(rotation(1,45),view(pose));
        pose.update(rotation(1,90),3);same(identity(),view(pose));
        float[] copy=view(pose);copy[0]=100;same(identity(),view(pose));
    }
    @Test public void rejectsInvalidDisplayRotations(){for(int invalid:new int[]{-1,4})try{new HeadPose().update(identity(),invalid);fail("Invalid orientation accepted");}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("rotation"));}}
}
