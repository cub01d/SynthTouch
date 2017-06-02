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
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

//import developer.shivam.library.WaveView;
import developer.shivam.library.WaveView;
import edu.ucsb.ece150.synthtouch.ball.BouncingBallView;
import edu.ucsb.ece150.synthtouch.ball.DrawingThread;
//import edu.ucsb.ece150.synthtouch.ball.SoundThread;

import android.os.Handler;


public class MainActivity extends Activity implements SensorEventListener {
    private int screenHeight;
    private int screenWidth;
    private float ballX = 120f;
    private float ballY = 140f;
    private float amp = 1.0f;
    private float phase = 0.0f;
    private float freq = 1000;
    private BouncingBallView bbv;
    private DrawingThread ballDrawingThread;
    private WaveView wave;
    private final double SAMPLE_INTERVAL = 0.2;       // sample every SAMPLE_INTERVAL seconds
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;

        // == bouncing ball ========================================================================
        float speedx = 0f;
        float speedy = 0f;

        bbv = new BouncingBallView(this);
        bbv.setBallPosition(ballX, ballY);
        bbv.setBallSpeed(speedx, speedy);
        fl.addView(bbv);

        ballDrawingThread = new DrawingThread(bbv, 60);
        ballDrawingThread.start();

        // == accelerometer ========================================================================
        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // register accelerometer sensor for SensorEventListener updates
        // check if sensor is supported
        if (!mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST)) {
            Toast.makeText(this, "Accelerometer is not supported on this device!", Toast.LENGTH_LONG).show();
        }

        // == background sine wave animation =======================================================
        wave = (WaveView) findViewById(R.id.waveview);
        wave.setSpeed(1);

        // == modulate the sound ===================================================================
        mHandler = new Handler();
        startSound();
//        SoundThread soundThread;
        // modulate the sound
//        Point p = new Point();
//        getWindowManager().getDefaultDisplay().getSize(p);
//        soundThread = new SoundThread(p.x, TIME_DURATION);
//        soundThread.start();
//        Thread soundModulator = new Thread(new Runnable() {
//        @Override
//        public void run() {
//            playSound(freq , 44100/10/*, phi*/);
//            Log.d("Handlers", "Called on main thread");
//            // Repeat this the same runnable code block again another 2 seconds
//            handler.postDelayed(soundModulator, 1);
//        }
//    });
//        Thread soundModulator = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                double freq = (double) ballX;
//                double phase = (double)
//            }
//        }).start();
//        updateThread.start();




        // TODO: background color change
        // private DrawingThread backgroundColorThread;
    }

    Runnable soundModulator = new Runnable() {
        @Override
        public void run() {
            try {
                float f = freq;             // freq may update while we are generating our wave
                // AudioTrack definition
                int mBufferSize = AudioTrack.getMinBufferSize(44100,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_8BIT);

                AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        mBufferSize, AudioTrack.MODE_STREAM);

                // generate our sine wave
                double[] mSound = new double[(int) (SAMPLE_INTERVAL * 44100)];
                short[] mBuffer = new short[(int) (SAMPLE_INTERVAL * 44100)];

                for (int i = 0; i < mSound.length; i++) {
                    mSound[i] = Math.sin(2.0*Math.PI * i/(44100/f) + phase);  // range: [-1, 1]
                    mBuffer[i] = (short) (mSound[i]*Short.MAX_VALUE);
                }

                // TODO: fix phase to avoid clicks
//                phase = (float) ((f * SAMPLE_INTERVAL % 1) * f * 2 * Math.PI) + 20;
                mAudioTrack.setVolume(amp * AudioTrack.getMaxVolume());
                mAudioTrack.play();

                mAudioTrack.write(mBuffer, 0, mSound.length, AudioTrack.WRITE_NON_BLOCKING);
                mAudioTrack.stop();
                mAudioTrack.release();

            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(soundModulator, (long) SAMPLE_INTERVAL * 1000);
            }
        }
    };

    void startSound() {
        soundModulator.run();
    }
    void stopSound() {
        mHandler.removeCallbacks(soundModulator);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ballDrawingThread.start();
    }
    @Override
    public void onStop() {
        super.onStop();

        // stop drawing threads
        ballDrawingThread.stop();
        // backgroundColorThread.stop();

        // stop sound
        stopSound();
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
//        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // correct for gravity
        accel[0] = event.values[0] - gravity[0];
        accel[1] = event.values[1] - gravity[1];
//        accel[2] = event.values[2] - gravity[2];

        // send accel info to bouncing ball
        // ball should move right (+x) when accel[1] is positive (lifting up top edge of phone)
        // ball should move left (-x) when accel[1] is negative (lifting up bottom edge of phone)
        // ball should move up (-y) when accel[0] is negative (lifting up left edge of phone)
        // ball should move down (+y) when accel[0] is positive (lifting up right edge of phone)
        ballX = bbv.getBallPosition()[0];
        ballY = bbv.getBallPosition()[1];
        Log.e("ball coords:", "x: " + ballX + ", y: " + ballY);

        if (!bbv.isFingerDown()) {
            bbv.setAccel(accel[1], accel[0]);
        }
        // set amplitude to y position
        wave.setAmplitude((int) (screenHeight-ballY)/100);
        amp = (screenHeight-ballY)/screenHeight;
        freq = 1000 + 2*ballX;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}