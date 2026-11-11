package de.meiapps.sternenreise;

/** Physical display and lens centers, independent of the virtual camera baseline. */
public final class ViewerOptics {
    private ViewerOptics() { }
    public static float displayWidthMm(int pixels,float xdpi,float ydpi) {
        float dpi=(xdpi+ydpi)/2;
        float measured=pixels*25.4f/dpi;
        return validWidth(measured)?measured:150;
    }
    public static float screenWidthMm(float configured,int pixels,float xdpi,float ydpi) {
        return validWidth(configured)?configured:displayWidthMm(pixels,xdpi,ydpi);
    }
    private static boolean validWidth(float width) { return Float.isFinite(width)&&width>=100&&width<=240; }
    public static float lensDistanceMm(float distance) { return Float.isFinite(distance)?Math.max(40,Math.min(90,distance)):64; }
    public static float centerX(int eye,float screenWidthMm,float spacingMm) {
        float width=validWidth(screenWidthMm)?screenWidthMm:150;
        float left=1-2*lensDistanceMm(spacingMm)/width;
        return eye==0?left:-left;
    }
    public static float centerY(float offsetMm,float screenHeightMm) {
        if(!Float.isFinite(offsetMm)||!Float.isFinite(screenHeightMm)||screenHeightMm<=0)return 0;
        return 2*Math.max(-10,Math.min(10,offsetMm))/screenHeightMm;
    }
}
