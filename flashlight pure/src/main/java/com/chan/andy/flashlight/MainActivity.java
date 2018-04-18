package com.chan.andy.flashlight;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.FloatMath;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements SensorEventListener{
    Camera mCamera;
    boolean flashOn = false;
    StrobeRunner sr;
    SOSRunner sosR;
    SmartRunner smartR;
    MorseRunner morseR;
    Thread t;
    int freq;
    String text;
    boolean smartOn = false;

    private SensorManager sensorMan;
    private Sensor accelerometer;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (smartOn) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
                // Shake detection
                float x = mGravity[0];
                float y = mGravity[1];
                float z = mGravity[2];
                mAccelLast = mAccelCurrent;
                mAccelCurrent = FloatMath.sqrt(x * x + y * y + z * z);
                float delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta;
                // Make this higher or lower according to how much
                // motion you want to detect
                if (mAccel > .1) {
                    smartR.elapsedTime = 0L;
                    smartR.startTime = System.currentTimeMillis();
                    try {
                        Camera.Parameters torchOn = mCamera.getParameters();
                        torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(torchOn);
                        flashOn = true;
                    } catch (Exception e) {
                        mCamera = Camera.open();
                        mCamera.startPreview();
                        Camera.Parameters torchOn = mCamera.getParameters();
                        torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(torchOn);
                        flashOn = true;
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_UI);

        if (mCamera == null) {
            mCamera = Camera.open();
            mCamera.startPreview();
        }

        final TextView freqText = (TextView)findViewById(R.id.frequencyText);
        final EditText morseText = (EditText)findViewById(R.id.morseText);
        final Switch strobeSwitch = (Switch)findViewById(R.id.strobeSwitch);
        final Switch toggleSwitch = (Switch)findViewById(R.id.toggleSwitch);
        final Switch smartSwitch = (Switch)findViewById(R.id.smartSwitch);
        final Switch sosSwitch = (Switch)findViewById(R.id.sosSwitch);
        final Switch morseSwitch = (Switch)findViewById(R.id.morseSwitch);
        SeekBar skBar = (SeekBar)findViewById(R.id.seekBar);

        skBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freq = progress;
                if (sr != null) {
                    sr.freq = freq;
                }
                freqText.setText((1000 - freq) + " ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (sosSwitch.isChecked()) {
                        sosSwitch.setChecked(false);
                    }
                    if (morseSwitch.isChecked()) {
                        morseSwitch.setChecked(false);
                    }
                    if (smartSwitch.isChecked()) {
                        smartSwitch.setChecked(false);
                    }
                    if (strobeSwitch.isChecked()) {
                        runStrobe();
                    } else {
                        Camera.Parameters torchOn = mCamera.getParameters();
                        torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(torchOn);
                        flashOn = true;
                    }
                } else {
                    try {
                        if (t != null) {
                            t.interrupt();
                            t = null;
                            Camera.Parameters torchOff = mCamera.getParameters();
                            torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            mCamera.setParameters(torchOff);
                            flashOn = false;
                        } else {
                            Camera.Parameters torchOff = mCamera.getParameters();
                            torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            mCamera.setParameters(torchOff);
                            flashOn = false;
                        }
                    } catch (Exception e){
                        mCamera = Camera.open();
                        mCamera.startPreview();
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    }
                }
            }
        });

        smartSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    smartOn = true;
                    if (toggleSwitch.isChecked()) {
                        toggleSwitch.setChecked(false);
                    }
                    if (sosSwitch.isChecked()) {
                        sosSwitch.setChecked(false);
                    }
                    if (morseSwitch.isChecked()) {
                        morseSwitch.setChecked(false);
                    }
                    runSmart();
                } else {
                    smartOn = false;
                    try {
                        if (t != null) {
                            smartR.terminate();
                            t = null;
                            Camera.Parameters torchOff = mCamera.getParameters();
                            torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            mCamera.setParameters(torchOff);
                            flashOn = false;
                        } else {
                            Camera.Parameters torchOff = mCamera.getParameters();
                            torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            mCamera.setParameters(torchOff);
                            flashOn = false;
                        }
                    } catch (Exception e){
                        mCamera = Camera.open();
                        mCamera.startPreview();
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    }
                }
            }
        });

        strobeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (toggleSwitch.isChecked()) {
                        runStrobe();
                    }
                } else {
                    if (toggleSwitch.isChecked()) {
                        t.interrupt();
                        t = null;
                        try {
                            Camera.Parameters torchOn = mCamera.getParameters();
                            torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            mCamera.setParameters(torchOn);
                        } catch (Exception e) {
                            mCamera = Camera.open();
                            mCamera.startPreview();
                            Camera.Parameters torchOn = mCamera.getParameters();
                            torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            mCamera.setParameters(torchOn);
                        }
                    }
                }
            }
        });

        sosSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (toggleSwitch.isChecked()) {
                        toggleSwitch.setChecked(false);
                    }
                    if (morseSwitch.isChecked()) {
                        morseSwitch.setChecked(false);
                    }
                    if (smartSwitch.isChecked()) {
                        smartSwitch.setChecked(false);
                    }
                    runSOS();
                } else {
                    try {
                        t.interrupt();
                        t = null;
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    } catch (Exception e) {
                        mCamera = Camera.open();
                        mCamera.startPreview();
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    }
                }
            }
        });

        morseSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (toggleSwitch.isChecked()) {
                        toggleSwitch.setChecked(false);
                    }
                    if (sosSwitch.isChecked()) {
                        sosSwitch.setChecked(false);
                    }
                    if (smartSwitch.isChecked()) {
                        smartSwitch.setChecked(false);
                    }
                    text = morseText.getText().toString();
                    runMorse();

                } else {
                    try {
                        t.interrupt();
                        t = null;
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    } catch (Exception e) {
                        mCamera = Camera.open();
                        mCamera.startPreview();
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    }
                }
            }
        });
    }


    private void runStrobe () {
        sr = new StrobeRunner();
        sr.freq = freq;
        t = new Thread(sr);
        t.start();
        flashOn = true;
    }

    private void runSmart () {
        smartR = new SmartRunner();
        t = new Thread(smartR);
        t.start();
    }

    private void runSOS () {
        sosR = new SOSRunner();
        t = new Thread(sosR);
        t.start();
        flashOn = true;
    }

    private void runMorse () {
        morseR = new MorseRunner();
        morseR.text = text.toLowerCase();
        t = new Thread(morseR);
        t.start();
        flashOn = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!flashOn) {
            // crashes without try-catch if !flashOn and backPress
            try {
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (smartR != null) {
            smartR.terminate();
            smartOn = false;
        }
        mCamera.release();
        mCamera = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required method
    }

    private class StrobeRunner implements Runnable {

        boolean stopRunning = false;
        int freq;

        @Override
        public void run() {
            Camera.Parameters torchOn = mCamera.getParameters();
            Camera.Parameters torchOff = mCamera.getParameters();
            torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            try {
                while (!stopRunning) {
                    mCamera.setParameters(torchOn);
                    Thread.sleep(1000 - freq);
                    mCamera.setParameters(torchOff);
                    Thread.sleep(1000 - freq);
                }
            }
            catch(Throwable e) {
            }
        }
    }

    private class SOSRunner implements Runnable {
        boolean stopRunning = false;
        Camera.Parameters torchOn = mCamera.getParameters();
        Camera.Parameters torchOff = mCamera.getParameters();
        @Override
        public void run() {
            torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            try {
                while (!stopRunning) {
                    dot();
                    Thread.sleep(200);
                    dot();
                    Thread.sleep(200);
                    dot();
                    Thread.sleep(800);
                    dash();
                    Thread.sleep(200);
                    dash();
                    Thread.sleep(200);
                    dash();
                    Thread.sleep(800);
                    dot();
                    Thread.sleep(200);
                    dot();
                    Thread.sleep(200);
                    dot();
                    Thread.sleep(1400);
                }
            }
            catch(Throwable e) {
            }
        }

        public void dot() throws InterruptedException {
            mCamera.setParameters(torchOn);
            Thread.sleep(100);
            mCamera.setParameters(torchOff);
        }

        public void dash() throws InterruptedException {
            mCamera.setParameters(torchOn);
            Thread.sleep(500);
            mCamera.setParameters(torchOff);
        }
    }

    private class MorseRunner implements Runnable {
        boolean running = true;
        String text;
        String[] alpha = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z", "1", "2", "3", "4", "5", "6", "7", "8",
                "9", "0"};
        String[] dottie = { ".-", "-...", "-.-.", "-..", ".", "..-.", "--.",
                "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.",
                "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-",
                "-.--", "--..", ".----", "..---", "...--", "....-", ".....",
                "-....", "--...", "---..", "----.", "-----"};
        Camera.Parameters torchOn = mCamera.getParameters();
        Camera.Parameters torchOff = mCamera.getParameters();
        @Override
        public void run() {
            String[] ary = text.split("(?!^)");
            torchOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            try {
                for (int i = 0; i < ary.length; i++)
                {
                    if (!ary[i].equals(" ")) {
                        String dot = dottie[java.util.Arrays.asList(alpha).indexOf(ary[i])];
                        for (int j = 0; j < dot.length(); j++) {
                            if (dot.charAt(j) == '.') {
                                dot();
                            }
                            if (dot.charAt(j) == '-') {
                                dash();
                            }
                            if (j != dot.length() - 1) {
                                Thread.sleep(200);
                            } else {
                                Thread.sleep(800);
                            }
                        }
                    } else {
                        Thread.sleep(1400);
                    }
                }
                running = false;
            }
            catch(Throwable e) {
                running = false;
            }
        }

        public void dot() throws InterruptedException {
            mCamera.setParameters(torchOn);
            Thread.sleep(100);
            mCamera.setParameters(torchOff);
        }

        public void dash() throws InterruptedException {
            mCamera.setParameters(torchOn);
            Thread.sleep(500);
            mCamera.setParameters(torchOff);
        }
    }

    private class SmartRunner implements Runnable {
        long elapsedTime;
        long startTime;
        boolean running = true;
        public void terminate() {
            running = false;
        }
        public void run() {

            while (running) {
                elapsedTime = (new Date()).getTime() - startTime;
                if (elapsedTime > 5000) {
                    try {
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    } catch (Exception e) {
                        mCamera = Camera.open();
                        mCamera.startPreview();
                        Camera.Parameters torchOff = mCamera.getParameters();
                        torchOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(torchOff);
                        flashOn = false;
                    }
                }
            }
        }
    }


}
