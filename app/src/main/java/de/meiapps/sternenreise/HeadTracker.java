package de.meiapps.sternenreise;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.WindowManager;

/** Converts the current display orientation and sensor basis before computing a camera pose. */
final class HeadTracker implements SensorEventListener {
    private final SensorManager manager;
    private final Sensor sensor;
    private final Display display;
    private final HeadPose pose=new HeadPose();
    private final float[] sensorRows=new float[16];
    @SuppressWarnings("deprecation")
    HeadTracker(Context context) {
        manager=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        Sensor candidate=manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensor=candidate!=null?candidate:manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        display=((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }
    boolean available() { return sensor!=null; }
    void start() { if(sensor!=null)manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME); }
    void stop() { manager.unregisterListener(this); }
    synchronized void center() { pose.reset(); }
    synchronized void copyView(float[] output) { pose.copyView(output); }
    @Override public synchronized void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(sensorRows,event.values);
        pose.update(sensorRows,display.getRotation());
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) { }
}
