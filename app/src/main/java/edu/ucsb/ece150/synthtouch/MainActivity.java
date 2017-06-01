package edu.ucsb.ece150.synthtouch;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import developer.shivam.library.WaveView;
import edu.ucsb.ece150.synthtouch.ball.BouncingBallView;
import edu.ucsb.ece150.synthtouch.ball.DrawingThread;
import android.os.Handler;


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

    private WaveView wave;

    double freq = 1000;

    // TODO: sine wave
//    private WaveView wv;
//    private DrawingThread waveDrawingThread;
    // TODO: background color change
//    private DrawingThread backgroundColorThread;

    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            playSound(freq , 44100/10);
            Log.d("Handlers", "Called on main thread");
            // Repeat this the same runnable code block again another 2 seconds
            handler.postDelayed(runnableCode, 1);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);

        wave = (WaveView) findViewById(R.id.sample_wave_view);

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

        handler.post(runnableCode);



        // TODO: background color, sine wave animation
        // setup other threads, etc

        // start playing sound
//        while true
//        do stuff


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

        //wave.setSpeed(bbv.getBallPosition()[0]/100);
        wave.setSpeed(1);
        wave.setAmplitude((int)bbv.getBallPosition()[1]/50);

        freq = (double)bbv.getBallPosition()[0];
    }

    private void playSound(double frequency, int duration) {
        // AudioTrack definition
        int mBufferSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_8BIT);

        AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                mBufferSize, AudioTrack.MODE_STREAM);

        // Sine wave
        double[] mSound = new double[4410];
        short[] mBuffer = new short[duration];
        for (int i = 0; i < mSound.length; i++) {
            mSound[i] = Math.sin((2.0*Math.PI * i/(44100/frequency)));
            mBuffer[i] = (short) (mSound[i]*Short.MAX_VALUE);
        }

        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
        mAudioTrack.play();

        mAudioTrack.write(mBuffer, 0, mSound.length);
        mAudioTrack.stop();
        mAudioTrack.release();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}