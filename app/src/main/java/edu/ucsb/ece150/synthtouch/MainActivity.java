package edu.ucsb.ece150.synthtouch;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import edu.ucsb.ece150.synthtouch.ball.BouncingBallView;
import edu.ucsb.ece150.synthtouch.ball.DrawingThread;

public class MainActivity extends Activity implements SensorEventListener {

    // ball properties
    private float x = 120f;                     // initial position
    private float y = 140f;
    private float speedx = 0f;
    private float speedy = 0f;
    private BouncingBallView bbv;
    private DrawingThread ballDrawingThread;

    // accelerometer
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // TODO: sine wave
//    private WaveView wv;
//    private DrawingThread waveDrawingThread;
    // TODO: background color change
//    private DrawingThread backgroundColorThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);

        // set up accelerometer
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // register accelerometer sensor for SensorEventListener updates
        // check if sensor is supported
        if (!mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST)) {
            Toast.makeText(this, "Accelerometer is not supported on this device!", Toast.LENGTH_LONG).show();
        }


        // bouncing ball
        bbv = new BouncingBallView(this);
        bbv.setBallPosition(x, y);
        bbv.setBallSpeed(speedx, speedy);
        fl.addView(bbv);
        ballDrawingThread = new DrawingThread(bbv, 50);
        ballDrawingThread.start();

        // TODO: background color, sine wave animation
        // setup other threads, etc

        // TODO: play sound

    }

    @Override
    public void onStop() {
        super.onStop();

        // stop drawing threads
        ballDrawingThread.stop();
        // waveDrawingThread.stop();
        // backgroundColorThread.stop();
    }

    // accelerometer SensorEventListener methods
    @Override
    public void onSensorChanged(SensorEvent event) {
        float gravity[] = new float[3];
        float accel[] = new float[3];

        // get sensor information
        final float alpha = (float) 0.8;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // correct for gravity
        accel[0] = event.values[0] - gravity[0];
        accel[1] = event.values[1] - gravity[1];
        accel[2] = event.values[2] - gravity[2];

        // send accel info to bouncing ball
        // ball should move right (+x) when accel[1] is positive (lifting up top edge of phone)
        // ball should move left (-x) when accel[1] is negative (lifting up bottom edge of phone)
        // ball should move up (-y) when accel[0] is negative (lifting up left edge of phone)
        // ball should move down (+y) when accel[0] is positive (lifting up right edge of phone)
        Log.e("onSensorChanged", "x: " + accel[1] + ", y: " + accel[0]);

        bbv.setAccel(accel[1], accel[0]);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}