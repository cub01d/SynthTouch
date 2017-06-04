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

import developer.shivam.library.WaveView;
import edu.ucsb.ece150.synthtouch.ball.BouncingBallView;
import edu.ucsb.ece150.synthtouch.ball.DrawingThread;

public class MainActivity extends Activity implements SensorEventListener {
    // screen dimensions
    private int screenHeight;
    // ball properties
    private float ballX = 120f;
    private float ballY = 140f;
    private BouncingBallView bbv;
    private DrawingThread ballDrawingThread;
    // sound properties
    private float amp = 1.0f;
    private float phase = 0.0f;
    private float freq = 1000;
    private boolean running;
    private Thread soundThread;
    final int SAMPLE_RATE = 44100;
    // wave animation
    private WaveView wave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        running = true;
        setContentView(R.layout.activity_main);
        FrameLayout fl = (FrameLayout) findViewById(R.id.frameLayout);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;

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
        soundThread = new Thread() {
            @Override
            public void run() {
                setPriority(Thread.MAX_PRIORITY);
                int buffsize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        buffsize, AudioTrack.MODE_STREAM);
                short samples[] = new short[buffsize];
                int amp = 10000;
                double twopi = 2 * Math.PI;

                // start audio
                audioTrack.play();

                // synthesis loop
                while (running) {
                    float f = freq;
                    for (int i = 0; i < buffsize; i++) {
                        samples[i] = (short)(amp * Math.sin(phase));
                        phase += twopi * f / SAMPLE_RATE;
                        phase %= twopi;
                    }
                    audioTrack.write(samples, 0, buffsize);
                    audioTrack.setVolume(MainActivity.this.getAmp() * AudioTrack.getMaxVolume());
                }
                audioTrack.stop();
                audioTrack.release();
            }
        };
        soundThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ballDrawingThread.start();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        // stop sound
        running = false;
        stopSound();

        // stop drawing threads
        ballDrawingThread.stop();
    }

    public float getAmp() {
        return amp;
    }

    void stopSound() {
        try {
            running = false;
            soundThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            soundThread = null;
        }
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

        // correct for gravity
        accel[0] = event.values[0] - gravity[0];
        accel[1] = event.values[1] - gravity[1];

        // send accel info to bouncing ball
        ballX = bbv.getBallPosition()[0];
        ballY = bbv.getBallPosition()[1];

        // don't send accel info if finger is dragging the ball
        if (!bbv.isFingerDown()) {
            bbv.setAccel(accel[1], accel[0]);
        }

        // set amplitude of wave animation, volume of sound to y position
        amp = (screenHeight-ballY)/((float) screenHeight);
        wave.setAmplitude((int) (screenHeight-ballY)/100);

        // set frequency of sound and speed of animation
        freq = 1000 + 2*ballX;
        wave.setSpeed(freq/800);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}