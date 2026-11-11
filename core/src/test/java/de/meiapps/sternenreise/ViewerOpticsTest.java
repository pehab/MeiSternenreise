package de.meiapps.sternenreise;
import org.junit.Test;
import static org.junit.Assert.*;
public class ViewerOpticsTest {
    @Test public void physicalCentersAre64MmApartRatherThanQuarterScreenCenters(){
        float width=150,spacing=64;
        float left=(ViewerOptics.centerX(0,width,spacing)+1)/2*(width/2);
        float right=width/2+(ViewerOptics.centerX(1,width,spacing)+1)/2*(width/2);
        assertEquals(43,left,.0001f);assertEquals(107,right,.0001f);assertEquals(spacing,right-left,.0001f);
        assertTrue(Math.abs(left-width/4)>5);
    }
    @Test public void centersMirrorAndHalfWidthLensesNeedNoShift(){
        for(float width:new float[]{120,150,200})for(float spacing:new float[]{50,64,80})assertEquals(-ViewerOptics.centerX(0,width,spacing),ViewerOptics.centerX(1,width,spacing),0);
        assertEquals(0,ViewerOptics.centerX(0,128,64),0);
        assertEquals(ViewerOptics.centerX(0,150,64),ViewerOptics.centerX(0,Float.NaN,Float.NaN),0);
    }
    @Test public void dimensionsAndSpacingAreBounded(){
        assertEquals(152.4f,ViewerOptics.displayWidthMm(2400,400,400),.001f);
        for(float dpi:new float[]{0,-100,Float.NaN,Float.POSITIVE_INFINITY,20,10000})assertEquals(150,ViewerOptics.displayWidthMm(2400,dpi,dpi),0);
        assertEquals(160,ViewerOptics.screenWidthMm(160,2400,400,400),0);
        assertEquals(152.4f,ViewerOptics.screenWidthMm(0,2400,400,400),.001f);
        assertEquals(40,ViewerOptics.lensDistanceMm(20),0);assertEquals(90,ViewerOptics.lensDistanceMm(100),0);assertEquals(64,ViewerOptics.lensDistanceMm(Float.NaN),0);
        assertEquals(.1f,ViewerOptics.centerY(3,60),.0001f);assertEquals(-1/3f,ViewerOptics.centerY(-50,60),.0001f);
        assertEquals(1/3f,ViewerOptics.centerY(50,60),.0001f);
        assertEquals(0,ViewerOptics.centerY(Float.NaN,60),0);assertEquals(0,ViewerOptics.centerY(3,0),0);assertEquals(0,ViewerOptics.centerY(3,Float.NaN),0);
    }
}
