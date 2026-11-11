package de.meiapps.sternenreise;

/** Android sensor matrices are row-major; OpenGL camera matrices are column-major. */
public final class HeadPose {
    private final float[] cameraToWorld=new float[16],referenceInverse=new float[16];
    private final float[] relative=new float[16],view=new float[16];
    private boolean initialized;
    private int rotation=-1;
    public HeadPose() { reset(); }
    public void reset() { initialized=false;rotation=-1;identity(view); }
    public void copyView(float[] destination) { System.arraycopy(view,0,destination,0,16); }
    public void update(float[] sensorRows,int displayRotation) {
        if(displayRotation<0||displayRotation>3)throw new IllegalArgumentException("Display rotation must be 0..3");
        identity(cameraToWorld);
        for(int row=0;row<3;row++) {
            float x=sensorRows[row*4],y=sensorRows[row*4+1];
            cameraToWorld[row]=switch(displayRotation){case 1->-y;case 2->-x;case 3->y;default->x;};
            cameraToWorld[4+row]=switch(displayRotation){case 1->x;case 2->-y;case 3->-x;default->y;};
            cameraToWorld[8+row]=sensorRows[row*4+2];
        }
        if(!initialized||rotation!=displayRotation) {
            transpose(referenceInverse,cameraToWorld);initialized=true;rotation=displayRotation;
        }
        // Relative camera orientation is reference^-1 * current; the view is its inverse.
        multiply(relative,referenceInverse,cameraToWorld);transpose(view,relative);
    }
    private static void identity(float[] matrix) {
        java.util.Arrays.fill(matrix,0);matrix[0]=matrix[5]=matrix[10]=matrix[15]=1;
    }
    private static void transpose(float[] result,float[] input) {
        for(int row=0;row<4;row++)for(int column=0;column<4;column++)result[column*4+row]=input[row*4+column];
    }
    private static void multiply(float[] result,float[] left,float[] right) {
        for(int column=0;column<4;column++)for(int row=0;row<4;row++) {
            float value=0;for(int index=0;index<4;index++)value+=left[index*4+row]*right[column*4+index];
            result[column*4+row]=value;
        }
    }
}
