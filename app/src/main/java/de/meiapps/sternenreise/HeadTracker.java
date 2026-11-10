package de.meiapps.sternenreise;

import android.content.Context;
import android.hardware.*;
import android.opengl.Matrix;

/** Landscape display coordinates; poses are copied atomically to the GL thread. */
final class HeadTracker implements SensorEventListener {
    private final SensorManager manager;
    private final Sensor sensor;
    private final float[] view = new float[16], inverseReference = new float[16];
    private boolean referenceReady;
    HeadTracker(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor candidate = manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensor = candidate != null ? candidate : manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Matrix.setIdentityM(view,0);
    }
    boolean available() { return sensor != null; }
    void start() { if (sensor!=null) manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME); }
    void stop() { manager.unregisterListener(this); }
    synchronized void center() { referenceReady=false; }
    synchronized void copyView(float[] out) { System.arraycopy(view,0,out,0,16); }
    @Override public synchronized void onSensorChanged(SensorEvent event) {
        float[] raw=new float[16], landscape=new float[16], relative=new float[16];
        SensorManager.getRotationMatrixFromVector(raw,event.values);
        SensorManager.remapCoordinateSystem(raw,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,landscape);
        if (!referenceReady) { Matrix.transposeM(inverseReference,0,landscape,0); referenceReady=true; }
        Matrix.multiplyMM(relative,0,inverseReference,0,landscape,0);
        Matrix.transposeM(view,0,relative,0);
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) { }
}
